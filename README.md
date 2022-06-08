# multibranch-build-regex-filter-extension

## Purpose
This plugin provides additional configuration that allows you to filter multi branch builds by file masks using a regex pattern for Bitbucket Server.
Supports PRs and direct push filtering. 
It utilizes the bitbucket REST API to diff change sets efficiently.
Useful in large repositories (monorepos) with many CI jobs. 
Inspired and stubbed by https://wiki.jenkins.io/display/JENKINS/Multibranch+Build+Strategy+Extension+Plugin


## Setup
On multibranch job go to Build Strategy section , click add button and select
Run build on any regex match strategy

![Multibranch build strategy extension](/images/exclude.png)

Fill the textarea with RegEx patterns:
^folder1/.*$ \
^.+/subfolder/.*$

