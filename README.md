# subspace.ai - PKM + REPL + AI

The long-term goal of subspace is to be/have three things:

* PKM (Personal Knowledge Management system) like Roam Research or Tana. 
* REPL-like (Read Evaluate Print Loop) capabilities. Should be able to execute individual code cells in the JVM backend and rendered in the frontend with [Electric](https://github.com/hyperfiddle/electric). Similar behaviour can be achieved with other languages via Jupyter kernels (or GraalVM Polyglot) and JavaScript.
* AI (Artificial Intelligence) integrations. Should be integrated with LLMs - e.g. write GPT queries in subspace, and incorporate the response to your personal knowledge base as a new node. Intelligent search and LLM-based summaries and reasoning over the existing knowledge base (Retrieval Oriented Generation, RAG).

The overall design should be open-ended, allowing for easy forking and providing custom node types / rendering functions. The goal is not to be just a storage of information, but a control panel for commonly used workflows. So that you can create convenient shortcuts and informative output views with Clojure + Electric. Since you persist which actions you took over time, you can search for past outputs and interleave these with your personal notes. Later query your knowledge base with RAG in natural language, or query it with GPT by exposing subspace knowledge base as an API to GPT.

For example, additional customizations and use cases could be:

* Intelligent work log for day to day coding.
* Wrappers for any babashka / shell scripts you already have.
* Wrapper functions to MLOps platform (or some other task manager) to trigger jobs, query stats and logs from past train runs. Build dashboards as subspace nodes from the result of such queries with Electric+HTML.
* Wrappers for common Kubernetes / AWS / GCP commands. Build ad hoc UIs on top of your cluster that make sense to *you*.
* Wrappers that pull the contents of arxiv documents as subspace nodes.
* Spaced repetition learning of content (of nodes which you mark to be remembered).

## UI/UX

There will be two types of UI elements: pages and nodes. Pages contain nodes, and nodes can nest other nodes. Both pages and nodes are referencable (meaning you can link to them and the page/node will get a backreference).

Each node contains some media, and possibly subnodes.

Media can be:
1. Text, numeric, Markdown
2. Image, video, audio
3. Flexible spreadsheet [tesserrae](https://github.com/lumberdev/tesserae)
4. code block, which can be executed in a jupyter kernel (runs once)
5. code block containing an e/fn (runs continuously when on the page)

Executing an e/fn is the most powerful and flexible thing to do. It can pull data in from other nodes on the page or in the graph, and displays its own little UI within its boundaries. Crucially, when upstream info changes, your e/fn's output gets recomputed. Running tesserrae is also very powerful; you can think of subspace as a non-grid tesserae that can also embed tesserae.

Subnodes can be organised either by indenting or tiling. 
1. Indented - subnodes are nested via indentation (like sub bullet points)
2. Tiled - subnodes are split horizontally or vertically

Each node is configured separately which of the three subnode organisation approaches is used. For example, a single page can have several tiles to split up the area into broad regions (using horizontal and vertical tiling) and within each tile we have nested bullet points of text (indented way of organising subnodes).

## Setup

```
brew tap homebrew/cask-versions
brew install --cask temurin11
brew install clojure/tools/clojure

brew install jenv
jenv add /Library/Java/JavaVirtualMachines/temurin-11.jdk/Contents/Home/
```

## Running:

```
$ XTDB_ENABLE_BYTEUTILS_SHA1=true clj -A:dev -X user/main

Starting Electric compiler and server...
shadow-cljs - server version: 2.20.1 running at http://localhost:9630
shadow-cljs - nREPL server started on port 9001
[:app] Configuring build.
[:app] Compiling ...
[:app] Build completed. (224 files, 0 compiled, 0 warnings, 1.93s)

👉 App server available at http://0.0.0.0:8080
```

## Why the name?

In mathematics, a *subspace* is some region in a vector space. In machine learning, we often operate with neural embedding vectors (of text, images, etc) where embeddings of similar concepts have vectors nearby in this "[latent space](https://en.wikipedia.org/wiki/Latent_space)".

As a metaphor, you can imagine each node in your PKM tool to occupy one point in a very high-dimensional latent space of knowledge. Navigating to a node in the PKM is like zooming in to a region in this latent space, which contains many nodes (points) nested under the main node - so in a way, you're viewing a subspace of your knowledge base.

## Status

Almost nothing started, but might scope out the UI when Electric v3 comes out.

## Credits

* Adapted from [electric-xtdb-started](https://github.com/hyperfiddle/electric-xtdb-starter)
* Which was adapted from [xtdb-in-a-box](https://github.com/xtdb/xtdb-in-a-box)

Contributions welcome! Raise an issue/PR or reach out via Twittwer @mattiasarro.
