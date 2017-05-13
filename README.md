# railway-oriented-programming

[![Build Status](https://travis-ci.org/HughPowell/railway-oriented-programming.svg?branch=master)](https://travis-ci.org/HughPowell/railway-oriented-programming)

A Clojure implementation of [Railway Oriented Programming](https://fsharpforfunandprofit.com/posts/recipe-part2/).  I strongly suggest reading and understanding the article before diving in with this library.

## Other Projects

There's a couple of other projects doing something very similar. [One](https://gist.github.com/ah45/7518292c620679c460557a7038751d6d) using the Clojure cats library and [another](https://github.com/jwillem/rop-clojure) that's under active development.

## Usage

For Leiningen, add the following to the dependencies key in project.clj

    :dependencies
        [...
         [railway-oriented-programming "0.1.0"]
         ...]

## Hacking on this Library

To download and install the library locally

    git clone git@github.com:HughPowell/railway-oriented-programming.git
    cd railway-oriented-programming
    lein install
    cd ..

To then use it in your project add it to the projects dependencies in project.clj

    :dependencies
        [...
         [railway-oriented-programming "0.1.0-SNAPSHOT"]
         ...]

## Ownership and License

The contributors are listed in AUTHORS. This project uses the MPL v2 license, see LICENSE.

railway-oriented-programming uses the C4.1 (Collective Code Construction Contract) process for contributions.

railway-oriented-programming uses the clojure-style-guide for code style.

To report an issue, use the railway-oriented-programming issue tracker at github.com.
