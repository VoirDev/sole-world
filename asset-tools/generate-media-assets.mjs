import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import sharp from 'sharp';

const TOOL_DIR = path.dirname(fileURLToPath(import.meta.url));
const ROOT_DIR = path.resolve(TOOL_DIR, '..');

const ASSET_GROUPS = [
  {type: 'flags', dir: 'assets/flags', includeJpg: true},
  {type: 'cryptos', dir: 'assets/cryptos', includeJpg: false},
];

const DATA_OUTPUT_FILE = 'data/media_assets.json';

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

  const [, description, ratioSuffix] = match;
  const imageAspectRatio = ratioSuffix === '1x1' ? 'square' : 'wide';
  const sizes = imageAspectRatio === 'square' ? SQUARE_SIZES : WIDE_SIZES;

  return {
    description,
    ratioSuffix,
    imageAspectRatio,
    sizes,
    svgFileName: fileName,
  };
};

const readExistingAssets = async () => {
  try {
    const content = await fs.readFile(path.join(ROOT_DIR, DATA_OUTPUT_FILE), 'utf8');
    const assets = JSON.parse(content);

    return new Map(
      assets.map((asset) => [
        `${asset.imageFormats.svg}|${asset.imageAspectRatio}|${asset.description}`,
        asset.id,
      ]),
    );
  } catch (error) {
    if (error.code === 'ENOENT') {
      return new Map();
    }

    throw error;
  }
};

const nextAvailableId = (usedIds) => {
  let id = 1;

  while (usedIds.has(id)) {
    id += 1;
  }

  return id;
};

const buildEntryId = (existingIds, usedIds, svgPath, imageAspectRatio, description) => {
  const existingId = existingIds.get(`${svgPath}|${imageAspectRatio}|${description}`);

  if (existingId !== undefined) {
    usedIds.add(existingId);
    return existingId;
  }

  const id = nextAvailableId(usedIds);
  usedIds.add(id);
  return id;
};

const removeFileIfExists = async (filePath) => {
  try {
    await fs.unlink(filePath);
  } catch (error) {
    if (error.code !== 'ENOENT') {
      throw error;
    }
  }
};

const renderRasterAssets = async (
  svgBuffer,
  outputDir,
  outputRelDir,
  description,
  sizes,
  includeJpg,
) => {
  const formats = {
    png: {},
    jpg: includeJpg ? {} : null,
    webp: {},
  };

  for (const {key, width, height} of sizes) {
    const baseName = `${description}_${width}x${height}`;

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
  const fileNames = await fs.readdir(path.join(ROOT_DIR, dir));

  return fileNames
    .filter((fileName) => fileName.endsWith('.svg'))
    .map((fileName) => ({fileName, asset: parseSvgName(fileName)}))
    .filter(({asset}) => asset !== null)
    .sort((left, right) => left.fileName.localeCompare(right.fileName));
};

const main = async () => {
  console.log('Start generating assets...');

  const existingIds = await readExistingAssets();
  const usedIds = new Set();
  const resultJson = [];

  for (const group of ASSET_GROUPS) {
    const svgFiles = await collectSvgFiles(group.dir);
    console.log(`Processing ${svgFiles.length} ${group.type} SVGs from ${group.dir}`);

    for (const {asset} of svgFiles) {
      const svgPath = `${group.dir}/${asset.svgFileName}`;
      const outputDir = path.join(ROOT_DIR, group.dir);
      const svgBuffer = await fs.readFile(path.join(ROOT_DIR, svgPath));
      const rasterFormats = await renderRasterAssets(
        svgBuffer,
        outputDir,
        group.dir,
        asset.description,
        asset.sizes,
        group.includeJpg,
      );

      resultJson.push({
        id: buildEntryId(
          existingIds,
          usedIds,
          svgPath,
          asset.imageAspectRatio,
          asset.description,
        ),
        type: 'image',
        imageAspectRatio: asset.imageAspectRatio,
        imageFormats: {
          svg: svgPath,
          ...rasterFormats,
        },
        description: asset.description,
      });
    }
  }

  resultJson.sort((left, right) => left.id - right.id);

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
