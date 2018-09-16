import plugin, { StartPlugin } from '@start/plugin'

export default function parallel(...targets: StartPlugin[]) {
  return plugin('parallel', async context => {
    await Promise.all(
      targets.map(async t => {
        const targetRunner = await t
        await targetRunner(context)
      })
    )

    return context
  })
}
