# Assets

Flag and cryptocurrency images served by the API, and mirrored to the `sole-world` R2 bucket.

```text
flags/     Country and territory flags: {key}_1x1.svg, {key}_4x3.svg and their PNG, JPG and WebP renditions
cryptos/   Cryptocurrency logos: {key}_1x1.svg and their PNG and WebP renditions
```

The SVGs are the source. Every raster file is generated from them by
[`asset-tools/generate-media-assets.mjs`](../asset-tools/generate-media-assets.mjs), which also
rewrites [`data/media_assets.json`](../data/media_assets.json), the index the API loads. The index
records each path relative to this folder, `flags/us_4x3.svg`, which is also its key in the bucket.
The API publishes it as `/assets/flags/us_4x3.svg` when it serves the files itself, and as
`$ASSET_BASE_URL/flags/us_4x3.svg` when the bucket does.

## The bucket

The bucket holds the two folders at its root, keyed exactly as they are on disk:

```text
sole-world/
  cryptos/aave_1x1.svg
  cryptos/aave_128x128.png
  flags/us_4x3.svg
  ...
```

Uploads use Wrangler, one file per call. Run `npx wrangler login` once first.

### The upload loop

Every command below pipes a list of paths into the same loop. Define it once per shell session,
from inside `assets/`:

```bash
cd assets

r2_put() {
  while read -r f; do
    case "$f" in
      *.svg)  ct=image/svg+xml ;;
      *.png)  ct=image/png ;;
      *.jpg)  ct=image/jpeg ;;
      *.webp) ct=image/webp ;;
      *)      echo "skipping $f"; continue ;;
    esac
    npx wrangler r2 object put "sole-world/$f" --file "$f" \
      --content-type "$ct" --cache-control "public, max-age=86400" --remote
  done
}
```

Two flags matter:

- `--content-type`. Wrangler does not infer it from the extension. Without it, the object is stored
  with no type, and browsers refuse to render an SVG served that way.
- `--cache-control`. The file names carry no version, so a regenerated image keeps its URL. Browsers
  cache these files for one day, which limits how long anyone sees an old image after a change. See
  [Regenerated images](#regenerated-images).

### Full upload

Use this for the first upload, or to rebuild the bucket from scratch:

```bash
find cryptos flags -type f ! -name ".DS_Store" | r2_put
```

It uploads about 9,800 files, and each call starts a new `npx` process, so expect it to run for a
long time. It is safe to interrupt and re-run, because uploading a file again overwrites it with the
same content.

## Tracking what the bucket holds

The `assets-r2` git tag marks the commit whose `assets/` folder is in the bucket. Every update
compares against it, so only the files that changed are uploaded.

Upload from a clean `assets/` folder, so the files on disk match the commit being tagged:

```bash
git status --porcelain -- .
```

If that prints nothing, the folder is clean. After an upload completes, move the tag to the
current commit:

```bash
git tag -f assets-r2 && git push -f origin assets-r2
```

Move it only after the whole upload has succeeded. If a run fails partway, the tag still points at
the previous upload, so re-running the same command sends the full set of changes again.

## Updating

Every change follows the same steps:

1. Add, replace or remove SVGs in `flags/` or `cryptos/`.
2. Regenerate the renditions and the index:
   ```bash
   cd asset-tools && npm ci && npm run generate:assets
   ```
3. Commit `assets/` and `data/media_assets.json` together.
4. Upload what changed since `assets-r2` (below), then move the tag.

Git compares file contents, so regenerating every image uploads only the files whose bytes actually
changed.

### New flags or cryptos

Upload the files that were added or modified since the last upload:

```bash
git diff --name-only --relative --no-renames --diff-filter=AM assets-r2 HEAD -- . | r2_put
```

Upload new files **before** releasing the API version whose `media_assets.json` references them, so
no published URL ever points at a missing file.

### Regenerated images

Use the same command as for new files. A regenerated file is a modified file:

```bash
git diff --name-only --relative --no-renames --diff-filter=AM assets-r2 HEAD -- . | r2_put
```

The URL does not change, so caches may still hold the old image:

- **Cloudflare's edge cache** (custom domain): purge the changed URLs, or everything if many
  changed, under *Caching → Configuration → Purge Cache* for the zone.
- **Browsers**: they refresh on their own within a day, the `max-age` set on upload. They cannot be
  purged.

If a regeneration touches most files, for example after a `sharp` upgrade changes the encoder output,
the diff lists most of the folder. That is expected; run it the same way.

### Removed or renamed images

`--no-renames` makes git report a rename as one deletion plus one addition. Upload the additions
first, as above, and then delete the objects that no longer exist:

```bash
git diff --name-only --relative --no-renames --diff-filter=D assets-r2 HEAD -- . \
  | while read -r f; do npx wrangler r2 object delete "sole-world/$f" --remote; done
```

Run the deletion only **after** every API deployment that references those files has been replaced,
because a running older release still publishes their URLs. R2 deletion is permanent.

### Checking the result

```bash
npx wrangler r2 object get sole-world/flags/us_4x3.svg --remote --pipe | head -c 200
```

Through the bucket's public domain, the response must carry the right type:

```bash
curl -sI https://<public-domain>/flags/us_4x3.svg | grep -i -E 'content-type|cache-control'
```
