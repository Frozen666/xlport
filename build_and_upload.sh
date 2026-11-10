# Deploys an updated image to the registry. Note that running this doesn't update
# the running instances. You have to also update their version tag.

# Start Docker (takes some time)
open /Applications/Docker.app

# Build the image locally (you need Docker installed and running). The war is built and
# tested inside Docker. --platform: the cluster runs x86_64; without it an image built on
# an Apple Silicon Mac is arm64 and fails in k8s with "exec format error".
docker build --platform=linux/amd64 -t eu.gcr.io/rapidcomputeengine/xlport:v2-118 . || exit 2

# Upload the image to the registry
docker push eu.gcr.io/rapidcomputeengine/xlport:v2-118 || exit 3



# ------------------------------------------------------------------------------------------------------------------------------------
# docker build -t xlport/xlport-repo:v4 . || exit 2
# killall Docker
# You can authenticate to push to our own Docker image repo with
#    gcloud auth configure-docker
# The image will show up here: https://console.cloud.google.com/kubernetes/images/tags/computer?location=EU&project=rapidcomputeengine
# You may need to configure docker to use credentials from gcloud to authorize with
#    gcloud auth configure-docker

