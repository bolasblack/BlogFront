if [ ! -d publish_repo ]; then
  git clone --depth=1 git@github.com:bolasblack/bolasblack.github.com.git publish_repo
fi

yarn build
find ./publish_repo -type f ! -path '*/.git/*' -delete
cp dist/* publish_repo

cd publish_repo
if ! git diff-index --quiet HEAD --; then
  git add -A
  git commit -m `date -Iseconds`
  git push --force
fi
