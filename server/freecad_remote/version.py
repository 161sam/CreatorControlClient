import os

NAME = "CreatorControlServer"
VERSION = "0.1.0-dev"


def get_git_sha() -> str | None:
    sha = os.getenv("CCC_GIT_SHA")
    if sha:
        return sha
    return None


def get_version_payload() -> dict:
    return {
        "name": NAME,
        "version": VERSION,
        "git_sha": get_git_sha(),
    }
