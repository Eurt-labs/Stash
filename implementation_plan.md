
# Download Engine Fixes & Enhancements

This plan addresses the three core issues with the PC downloader engine: quality limitations, missing metadata/thumbnails, and playlist subfolder organization.

## Proposed Changes

### 1. Fix Quality Selection (Client Spoofing)
**Context**: YouTube recently started heavily restricting 1080p, 2K, and 4K streams on standard web clients, returning only 720p or 360p streams. This is why high quality video wasn\'t working.
- **Change**: I will update the `yt-dlp` extractor arguments back to use modern mobile/TV clients (`youtube:player_client=ios,android,tv`) to bypass the resolution restriction and unlock 1080p+ streams.

### 2. Enable Native Metadata & Thumbnail Embedding
**Context**: The current `DownloadEngine.ts` does not pass the flags required for `yt-dlp` to embed metadata and thumbnails.
- **Change**: Add the following arguments to the `yt-dlp` command in `DownloadEngine.ts`:
  - `--embed-metadata`
  - `--embed-thumbnail`
  - `--parse-metadata "NA:%(meta_album)s"` (to prevent empty album errors)
  - `--convert-thumbnails jpg` (ensures maximum compatibility)

### 3. Playlist Subfolder Organization
**Context**: Currently, all files are downloaded flat into the chosen directory.
- **Change**: In `DownloadEngine.ts`, I will dynamically append the `trackInfo.playlistName` (or Artist name, as parsed by the engine) to the `outputDir`. This ensures that playlists and albums get their own dedicated subfolders automatically.

### [MODIFY] `src/main/features/downloader/DownloadEngine.ts`
- Update the `args` array in `download()` to include metadata flags.
- Re-introduce the `youtube:player_client=ios,android,tv` extractor arg.
- Modify the `outputDir` logic to create and use `path.join(outputDir, trackInfo.playlistName)` if available.

## User Review Required
> [!IMPORTANT]
> The `youtube:player_client=ios,android,tv` fallback might slightly impact initial stream fetch speed compared to the `default` client, but it is **strictly required** to unlock 1080p and 4K video downloads on YouTube today. Are you okay with this tradeoff?

## Verification Plan
After applying, I will run a build verify (`npm run build`) to ensure no syntax or typing issues were introduced in `DownloadEngine.ts`.

