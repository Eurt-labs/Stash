import path from 'path'
import os from 'os'

export const APP_CONSTANTS = {
  DEFAULT_DOWNLOAD_DIR: path.join(os.homedir(), 'Downloads', 'Stash'),
  DEFAULT_STASH_BIN_DIR: path.join(os.homedir(), '.stash', 'bin'),
  DEFAULT_CACHE_DIR: path.join(os.homedir(), '.stash_cache'),
  USER_AGENT: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
  YT_DLP_NIGHTLY_URL: 'https://github.com/yt-dlp/yt-dlp-nightly-builds/releases/latest/download/yt-dlp.exe',
  SOCKET_TIMEOUT_SEC: 30,
  RETRIES: 5,
  FRAGMENT_RETRIES: 5
} as const
