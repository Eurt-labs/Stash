import axios from 'axios'
import { app } from 'electron'
import { AppUpdateStatus } from '../../../shared/types'

export class AppUpdateChecker {
  public static readonly REPO_OWNER = 'Eurt-labs'
  public static readonly REPO_NAME = 'Stash'
  public static readonly REPO_URL = 'https://github.com/Eurt-labs/Stash'
  public static readonly API_URL = 'https://api.github.com/repos/Eurt-labs/Stash/releases/latest'

  /**
   * Retrieves the current app version
   */
  public static getCurrentVersion(): string {
    if (app) {
      const v = app.getVersion()
      if (v && v !== '0.0.0') return v
    }
    return '2.0.0'
  }

  /**
   * Checks GitHub for the latest release of Stash
   */
  public static async checkForUpdates(): Promise<AppUpdateStatus> {
    const currentVersion = this.getCurrentVersion()

    try {
      const response = await axios.get(this.API_URL, {
        headers: {
          'User-Agent': 'Stash-Media-Downloader',
          Accept: 'application/vnd.github.v3+json'
        },
        timeout: 10000
      })

      if (response.status === 200 && response.data) {
        const release = response.data
        const rawTag: string = release.tag_name || release.name || ''
        const latestVersion = rawTag.replace(/^v/i, '').trim()
        const releaseUrl: string = release.html_url || `${this.REPO_URL}/releases`
        const releaseNotes: string = release.body || ''
        const publishedAt: string = release.published_at || ''

        const isNewer = this.compareSemver(latestVersion, currentVersion) > 0

        return {
          currentVersion,
          latestVersion,
          hasUpdate: isNewer,
          releaseUrl,
          releaseNotes,
          publishedAt
        }
      }

      return {
        currentVersion,
        hasUpdate: false,
        releaseUrl: `${this.REPO_URL}/releases`
      }
    } catch (err: any) {
      console.warn('Failed to check for app update:', err?.message || err)
      return {
        currentVersion,
        hasUpdate: false,
        releaseUrl: `${this.REPO_URL}/releases`,
        error: err?.response?.status === 404 ? 'No published releases found yet on GitHub.' : (err?.message || 'Could not connect to GitHub.')
      }
    }
  }

  /**
   * Compares two semver strings (e.g. '2.0.0' vs '1.3.0')
   * Returns > 0 if v1 > v2, < 0 if v1 < v2, 0 if equal.
   */
  public static compareSemver(v1: string, v2: string): number {
    const clean1 = v1.replace(/^v/i, '').split('-')[0].trim()
    const clean2 = v2.replace(/^v/i, '').split('-')[0].trim()

    const parts1 = clean1.split('.').map((p) => parseInt(p, 10) || 0)
    const parts2 = clean2.split('.').map((p) => parseInt(p, 10) || 0)

    const maxLength = Math.max(parts1.length, parts2.length, 3)

    for (let i = 0; i < maxLength; i++) {
      const num1 = parts1[i] || 0
      const num2 = parts2[i] || 0
      if (num1 > num2) return 1
      if (num1 < num2) return -1
    }

    return 0
  }
}
