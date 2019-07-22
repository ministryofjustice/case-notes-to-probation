i.PHONY: all ecr-login build tag test push clean-remote clean-local

aws_region := eu-west-2
image := hmpps/casenotes

# casenotes_version should be passed from command line
all:
	$(MAKE) ecr-login
	$(MAKE) build
	$(MAKE) test
	$(MAKE) push
	$(MAKE) clean-remote
	$(MAKE) clean-local

ecr-login:
	$(shell aws ecr get-login --no-include-email --region ${aws_region})
	aws --region $(aws_region) ecr describe-repositories --repository-names "$(image)" | jq -r .repositories[0].repositoryUri > ecr.repo

build: ecr_repo = $(shell cat ./ecr.repo)
build:
	$(info Build of repo $(ecr_repo))
	docker build -t $(ecr_repo) --build-arg casenotes_VERSION=${casenotes_version}  -f Dockerfile.aws .

tag: ecr_repo = $(shell cat ./ecr.repo)
tag:
	$(info Tag repo $(ecr_repo) $(casenotes_version))
	docker tag $(ecr_repo) $(ecr_repo):$(casenotes_version)

test: ecr_repo = $(shell cat ./ecr.repo)
test:
	bash -c "GOSS_FILES_STRATEGY=cp GOSS_FILES_PATH="./docker/tests/" GOSS_SLEEP=5 dgoss run -e casenotes_BUILDTESTMODE=true $(ecr_repo):latest"

push: ecr_repo = $(shell cat ./ecr.repo)
push:
	docker tag  ${ecr_repo} ${ecr_repo}:${casenotes_version}
	docker push ${ecr_repo}:${casenotes_version}

clean-remote: untagged_images = $(shell aws ecr list-images --region $(aws_region) --repository-name "$(image)" --filter "tagStatus=UNTAGGED" --query 'imageIds[*]' --output json)
clean-remote:
	if [ "${untagged_images}" != "[]" ]; then aws ecr batch-delete-image --region $(aws_region) --repository-name "$(image)" --image-ids '${untagged_images}' || true; fi

clean-local: ecr_repo = $(shell cat ./ecr.repo)
clean-local:
	docker rmi ${ecr_repo}:latest
	docker rmi ${ecr_repo}:${casenotes_version}
	rm -f ./ecr.repo