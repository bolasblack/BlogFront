import { defineConfig } from 'vite'
import { resolve } from 'path'
import * as fs from 'fs'

const SHADOW_CLJS_OUT_PATH = '.shadow-cljs/browser-out'

export default defineConfig({
  root: '.',
  publicDir: 'assets',
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    rollupOptions: {
      input: {
        main: resolve(__dirname, 'index.html'),
      },
    },
  },
  server: {
    port: 12564,
    // Serve shadow-cljs output
    fs: {
      allow: ['.'],
    },
  },
  plugins: [
    // Serve shadow-cljs output files in dev mode
    {
      name: 'serve-shadow-cljs',
      configureServer(server) {
        server.middlewares.use((req, res, next) => {
          if (req.url === '/main.js' || req.url?.startsWith('/main.js?')) {
            const filePath = resolve(__dirname, SHADOW_CLJS_OUT_PATH, 'main.js')
            if (fs.existsSync(filePath)) {
              res.setHeader('Content-Type', 'application/javascript')
              fs.createReadStream(filePath).pipe(res)
              return
            }
          }
          next()
        })
      },
    },
    // Copy shadow-cljs output in production build
    {
      name: 'copy-shadow-cljs',
      closeBundle() {
        const srcPath = resolve(__dirname, SHADOW_CLJS_OUT_PATH, 'main.js')
        const destPath = resolve(__dirname, 'dist', 'main.js')
        if (fs.existsSync(srcPath)) {
          fs.copyFileSync(srcPath, destPath)
        }
      },
    },
  ],
})
