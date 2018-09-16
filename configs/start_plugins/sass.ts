import plugin, { StartFile } from '@start/plugin'
import { Options, Result } from 'sass'

export default function sass(options: Options) {
  return plugin('sass', async ({ files, logFile }) => {
    const sass = await import('sass')

    return {
      files: await Promise.all(
        (files || []).map(async (f: StartFile): Promise<StartFile> => {
          logFile(f.path)

          const { data: fileData } = f
          if (!fileData) throw new Error('Should read sass/scss files before compile')

          const { css, map } = await new Promise<Result>((resolve, reject) =>
            sass.render({
              ...options,
              data: fileData,
            }, (err, res) => {
              err ? reject(err) : resolve(res)
            })
          )

          return {
            path: f.path,
            data: css.toString(),
            map: map ? JSON.parse(map.toString()) : f.map,
          }
        }),
      ),
    }
  })
}
