import * as core from "@actions/core"
import {context, getOctokit} from "@actions/github"

const octokit = getOctokit(import.meta.env.GITHUB_TOKEN) as import("@octokit/plugin-rest-endpoint-methods/dist-types/types").Api
const selfBuild = import.meta.env.RELEASE_VERSION

async function clearOldAssets(tag: string, keep: number = 9) {
    const release = await octokit.rest.repos.getReleaseByTag({
        ...context.repo, tag
    })
    const assets = release.data.assets.sort((a, b) => a.name < b.name ? 1 : -1)
    if (assets.length <= keep) return
    const toRemove = assets.slice(keep)
    toRemove.push(...assets.filter(it => it.name.includes("server") && !it.name.includes(selfBuild)))
    console.log("ToRemove", toRemove.map(it => it.name))
    await Promise.all(toRemove.map(it =>
        octokit.rest.repos.deleteReleaseAsset({
            ...context.repo, asset_id: it.id
        })
    ))
}

await clearOldAssets(core.getInput("release") || import.meta.env.TAG)