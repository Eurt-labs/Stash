import { session, BrowserWindow } from 'electron'
import fs from 'fs'
import path from 'path'
import { app } from 'electron'
import { DownloadEngine } from './DownloadEngine'

export class CookieManager {
  public static async loginAndExtractCookies(parentWindow?: BrowserWindow | null): Promise<void> {
    return new Promise((resolve) => {
      const loginWindow = new BrowserWindow({
        parent: parentWindow || undefined,
        modal: !!parentWindow,
        width: 800,
        height: 800,
        title: 'YouTube Login',
        webPreferences: {
          nodeIntegration: false,
          contextIsolation: true,
          partition: 'persist:youtube' // Use persistent partition
        }
      })

      loginWindow.loadURL('https://accounts.google.com/ServiceLogin?service=youtube&continue=https://www.youtube.com/')

      loginWindow.on('closed', async () => {
        try {
          const ytSession = session.fromPartition('persist:youtube')
          const cookies = await ytSession.cookies.get({ domain: '.youtube.com' })
          
          if (cookies.length === 0) {
            console.log('No YouTube cookies found.')
            resolve()
            return
          }

          const userDataPath = app.getPath('userData')
          const cookiesPath = path.join(userDataPath, 'youtube_cookies.txt')

          let cookieText = "# Netscape HTTP Cookie File\n"
          cookieText += "# https://curl.haxx.se/rfc/cookie_spec.html\n\n"

          cookies.forEach(c => {
            const domain = c.domain?.startsWith('.') ? c.domain : `.${c.domain}`
            const flag = "TRUE"
            const path = c.path || '/'
            const secure = c.secure ? "TRUE" : "FALSE"
            const expiration = c.expirationDate ? Math.floor(c.expirationDate) : Math.floor(Date.now() / 1000) + 31536000
            cookieText += `${domain}\t${flag}\t${path}\t${secure}\t${expiration}\t${c.name}\t${c.value}\n`
          })

          fs.writeFileSync(cookiesPath, cookieText, 'utf-8')
          console.log(`Saved YouTube cookies to ${cookiesPath}`)
          
          DownloadEngine.cookiesFile = cookiesPath
        } catch (e) {
          console.error('Failed to extract cookies:', e)
        }
        resolve()
      })
    })
  }
}
