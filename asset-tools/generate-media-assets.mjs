import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import sharp from 'sharp';

const TOOL_DIR = path.dirname(fileURLToPath(import.meta.url));
const ROOT_DIR = path.resolve(TOOL_DIR, '..');

// Paths are recorded relative to this folder — `flags/us_1x1.svg` — the same keys the files have in
// the R2 bucket. The API adds `/assets/` itself when it serves them, and nothing when a CDN does.
const ASSETS_DIR = path.join(ROOT_DIR, 'assets');

// An asset's id names what it belongs to, its key and its shape — `flag-us-square` — so it is
// derived from the file rather than assigned, and regenerating can never renumber it.
const ASSET_GROUPS = [
  {type: 'flags', owner: 'flag', dir: 'flags', includeJpg: true},
  {type: 'cryptos', owner: 'crypto', dir: 'cryptos', includeJpg: false},
];

const DATA_OUTPUT_FILE = 'data/media_assets.json';

// An asset's description names what it shows, and what it shows is whatever record points at it.
// Reading those records here is what keeps the description from drifting back into being a slug.
const OWNER_FILES = [
  {file: 'data/flags.json', idFields: ['square', 'wide'], describe: (record, field) => `${record.caption} flag, ${field === 'square' ? 'square' : 'wide'}`},
  {file: 'data/cryptos.json', idFields: ['logoId'], describe: (record) => `${record.name} logo`},
];

const SQUARE_SIZES = [
  {key: 'xs', width: 64, height: 64},
  {key: 'sm', width: 128, height: 128},
  {key: 'md', width: 256, height: 256},
  {key: 'lg', width: 512, height: 512},
  {key: 'xl', width: 1024, height: 1024},
];

const WIDE_SIZES = [
  {key: 'xs', width: 64, height: 48},
  {key: 'sm', width: 128, height: 96},
  {key: 'md', width: 256, height: 192},
  {key: 'lg', width: 512, height: 384},
  {key: 'xl', width: 1024, height: 768},
];

const parseSvgName = (fileName) => {
  const parsed = path.parse(fileName);
  const match = parsed.name.match(/^(.*)_(1x1|4x3)$/);

  if (!match) {
    return null;
  }

  const [, key, ratioSuffix] = match;
  const imageAspectRatio = ratioSuffix === '1x1' ? 'square' : 'wide';
  const sizes = imageAspectRatio === 'square' ? SQUARE_SIZES : WIDE_SIZES;

  return {
    key,
    ratioSuffix,
    imageAspectRatio,
    sizes,
    svgFileName: fileName,
  };
};

const assetId = (owner, key, imageAspectRatio) => `${owner}-${key}-${imageAspectRatio}`;

const removeFileIfExists = async (filePath) => {
  try {
    await fs.unlink(filePath);
  } catch (error) {
    if (error.code !== 'ENOENT') {
      throw error;
    }
  }
};

const readOwnerDescriptions = async () => {
  const descriptions = new Map();

  for (const {file, idFields, describe} of OWNER_FILES) {
    let records;

    try {
      records = JSON.parse(await fs.readFile(path.join(ROOT_DIR, file), 'utf8'));
    } catch (error) {
      if (error.code === 'ENOENT') {
        continue;
      }

      throw error;
    }

    for (const record of records) {
      for (const field of idFields) {
        if (record[field] !== null && record[field] !== undefined) {
          descriptions.set(record[field], describe(record, field));
        }
      }
    }
  }

  return descriptions;
};

const renderRasterAssets = async (
  svgBuffer,
  outputDir,
  outputRelDir,
  assetKey,
  sizes,
  includeJpg,
) => {
  const formats = {
    png: {},
    jpg: includeJpg ? {} : null,
    webp: {},
  };

  // The size's own key names the rendition (xs, sm, ...); the asset's key names the file.
  for (const {key, width, height} of sizes) {
    const baseName = `${assetKey}_${width}x${height}`;

    const pngFile = `${baseName}.png`;
    const jpgFile = `${baseName}.jpg`;
    const webpFile = `${baseName}.webp`;

    await sharp(svgBuffer).resize(width, height).png().toFile(path.join(outputDir, pngFile));
    await sharp(svgBuffer).resize(width, height).webp().toFile(path.join(outputDir, webpFile));

    if (includeJpg) {
      await sharp(svgBuffer).resize(width, height).jpeg().toFile(path.join(outputDir, jpgFile));
      formats.jpg[key] = `${outputRelDir}/${jpgFile}`;
    } else {
      await removeFileIfExists(path.join(outputDir, jpgFile));
    }

    formats.png[key] = `${outputRelDir}/${pngFile}`;
    formats.webp[key] = `${outputRelDir}/${webpFile}`;
  }

  return formats;
};

const collectSvgFiles = async (dir) => {
  const fileNames = await fs.readdir(path.join(ASSETS_DIR, dir));

  return fileNames
    .filter((fileName) => fileName.endsWith('.svg'))
    .map((fileName) => ({fileName, asset: parseSvgName(fileName)}))
    .filter(({asset}) => asset !== null)
    .sort((left, right) => left.fileName.localeCompare(right.fileName));
};

const main = async () => {
  console.log('Start generating assets...');

  const ownerDescriptions = await readOwnerDescriptions();
  const resultJson = [];

  for (const group of ASSET_GROUPS) {
    const svgFiles = await collectSvgFiles(group.dir);
    console.log(`Processing ${svgFiles.length} ${group.type} SVGs from ${group.dir}`);

    for (const {asset} of svgFiles) {
      const svgPath = `${group.dir}/${asset.svgFileName}`;
      const outputDir = path.join(ASSETS_DIR, group.dir);
      const svgBuffer = await fs.readFile(path.join(ASSETS_DIR, svgPath));
      const rasterFormats = await renderRasterAssets(
        svgBuffer,
        outputDir,
        group.dir,
        asset.key,
        asset.sizes,
        group.includeJpg,
      );

      const id = assetId(group.owner, asset.key, asset.imageAspectRatio);

      resultJson.push({
        id,
        type: 'image',
        key: asset.key,
        // An asset nothing points at yet falls back to its key; adding the owning record and
        // re-running fills it in.
        description:
          ownerDescriptions.get(id) ?? `${asset.key} image, ${asset.imageAspectRatio}`,
        imageAspectRatio: asset.imageAspectRatio,
        imageFormats: {
          svg: svgPath,
          ...rasterFormats,
        },
      });
    }
  }

  // Ordered by code point, not by locale, so the file comes out the same on every machine.
  resultJson.sort((left, right) => (left.id < right.id ? -1 : left.id > right.id ? 1 : 0));

  await fs.writeFile(
    path.join(ROOT_DIR, DATA_OUTPUT_FILE),
    `${JSON.stringify(resultJson, null, 2)}\n`,
    'utf8',
  );

  console.log(`Done. Generated ${resultJson.length} media asset entries.`);
};

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
