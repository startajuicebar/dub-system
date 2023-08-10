# Dub Box

A dubbing system

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

### Development

1. Rename the `dev-config.edn.sample` to `dev-config.edn` in the root dir and update the vars

## Running

Using Calva fire up the start repl and connect command for best repl experience

Otherwise start lein manually

```bash
lein run
```

## Building

To build the uberjar

```bash
lein uberjar
```

To start the uberjar locally with env vars

```bash
java -Dconf=dev-config.edn -jar target/uberjar/dub-box.jar
```

## Testing

The project uses the [kaocha](https://github.com/lambdaisland/kaocha) test runner

Run from root

```bash
bin/kaocha --fail-fast --watch
```

## Local dev

Use ngrok to open a local tunnel

```bash
ngrok start --all
```
