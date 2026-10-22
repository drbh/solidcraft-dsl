# solidcraft-dsl

> [!CAUTION] this project is a personal experiment and is actively being iterated on and may change significantly without notice at any time - repo may be deprecated for a future version or replaced entirely.

this is a small domain specific language for writing declarative 3d models.

## why

I've been looking for a nice way to lift 3d modeling into a more declarative and programmatic approach. I want all the niceties of coding; design are just text files, they are reproducible, version controlled, and support computation. 

essentially I want to be able to produce my 3d models without any clicking (but have an escape hatch for manual adjustments if needed).

other things that I want (not all of these are implemented yet), are composability, reusability, a cli interface made for iterating (emit images/meshes quickly as files), its important we build over the file system and are stateless so we can squeeze the most out of modern llm's to do our 3d bidding.

so, this is an attempt!

## status

this project is still more of an experiment than anything - and so far I've used it to print a bunch of functional 3d models - however there are many rough edges, incomplete features, and potential breaking changes as I continue to iterate on the design and implementation.

also I want to shout out [solidcraft](https://github.com/tangency-labs/solidcraft) since it has been the major enabler for this project since it exposes a useful imperative api for a mostly full featured 3d modeling workflow. my experiences with other cad programming tools - is that they do not treat sketching as a first-class concept, which makes it difficult to move between the a programming approach and more traditional interactive modeling - solidcraft handles this well so our dsl can use a combination of sketching and parametric modeling seamlessly.

anyway, if you read this far - you might as well try to build one of the examples yourself!