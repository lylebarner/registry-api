# 🏃‍♀️ Continuous Integration and Delivery: Unstable
# =================================================
#
# Note: for this workflow to succeed, the following secrets must be installed
# in the repository or inherited from the organization:
#
# ``ADMIN_GITHUB_TOKEN``
#     A personal access token of a user with collaborator or better access to
#     the project repository. You can generate this by visiting GitHub →
#     Settings → Developer settings → Personal access tokens → Generate new
#     token. Give the token scopes on ``repo``, ``write:packages``,
#     ``delete:packages``, ``workflow``, and ``read:gpg_key``.
# ``CODE_SIGNING_KEY``
#     A *private* key with which we can sign artifacts.
# ``OSSRH_USERNAME``
#     Username for the Central Repository.
# ``OSSRH_USERNAME``
#     Password for the Central Repository.


---

name: 🤪 Unstable integration & delivery


# Driving Event
# -------------
#
# What event starts this workflow: a push to ``main`` (or ``master`` in old
# parlance).

on:
    push:
        branches:
            -   master


# What to Do
# ----------
#
# Round up, yee-haw!

jobs:
    unstable-assembly:
        name: 🧩 Unstable Assembly
        runs-on: ubuntu-latest
        if: github.actor != 'pdsen-ci'
        steps:
            -
                name: 💳 Checkout
                uses: actions/checkout@v2
                with:
                    lfs: true
            -
                name: 🤠 Roundup
                uses: NASA-PDS/roundup-action@master
                with:
                    assembly: unstable
                env:
                    ossrh_username: ${{secrets.OSSRH_USERNAME}}
                    ossrh_password: ${{secrets.OSSRH_PASSWORD}}
                    CODE_SIGNING_KEY: ${{secrets.CODE_SIGNING_KEY}}
                    ADMIN_GITHUB_TOKEN: ${{secrets.ADMIN_GITHUB_TOKEN}}


# -*- mode: yaml; indent: 4; fill-column: 120; coding: utf-8 -*-
