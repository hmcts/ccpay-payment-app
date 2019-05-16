#!/usr/bin/env sh

REPO_NAME=$(echo "$TRAVIS_REPO_SLUG" | cut -f2- -d/)
COMMIT_REQUIRED=false

for group in "$@"
do
  CURRENT_DOCS=$(curl https://hmcts.github.io/reform-api-docs/specs/"$REPO_NAME"."$group".json)
  NEW_DOCS=$(cat /tmp/swagger-specs."$group".json)

  if [ "$CURRENT_DOCS" != "$NEW_DOCS" ]; then
      if [ "$COMMIT_REQUIRED" = false ] ; then
          echo "Update reform-api-docs"
          mkdir swagger-staging
          cd swagger-staging
          git init

          git config user.name "Travis CI"
          git config user.email "travis@travis-ci.org"
          git remote add upstream "https://${GH_TOKEN}@github.com/hmcts/reform-api-docs.git"
          git pull upstream master
      fi

      TARGET_SPEC=docs/specs/"$REPO_NAME"."$group".json
      echo "$NEW_DOCS" > "$TARGET_SPEC"

      git add "$TARGET_SPEC"
      COMMIT_REQUIRED=true
  else
      echo "API Documentation for group $group is up to date."
  fi
done

if [ "$COMMIT_REQUIRED" = true ] ; then
    git commit -m "Update spec from $TRAVIS_REPO_SLUG build $TRAVIS_BUILD_NUMBER"
    git push --set-upstream upstream master
fi

exit 0
