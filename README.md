## LaserDisc

[![Continuous Integration](https://github.com/laserdisc-io/laserdisc/actions/workflows/ci.yml/badge.svg)](https://github.com/laserdisc-io/laserdisc/actions/workflows/ci.yml)
[![Known Vulnerabilities](https://snyk.io/test/github/laserdisc-io/laserdisc/badge.svg?targetFile=build.sbt)](https://snyk.io/test/github/laserdisc-io/laserdisc?targetFile=build.sbt)
[![Join the chat at https://gitter.im/laserdisc-io/laserdisc](https://badges.gitter.im/laserdisc-io/laserdisc.svg)](https://gitter.im/laserdisc-io/laserdisc?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

[![laserdisc-core Scala version support](https://index.scala-lang.org/laserdisc-io/laserdisc/laserdisc-core/latest-by-scala-version.svg?targetType=Js)](https://index.scala-lang.org/laserdisc-io/laserdisc/laserdisc-core)


LaserDisc is a(nother) Scala driver for [Redis](https://redis.io/), written in Scala from the ground up.

It differentiates itself from the others for having a core layer, which is made up of all the supported Redis commands
and the Redis Serialization Protocol ([RESP](https://redis.io/topics/protocol)), that is strongly typed and which makes
heavy use of [shapeless](https://github.com/milessabin/shapeless) and [refined](https://github.com/fthomas/refined) to
achieve this. It also provides an implementation of RESP built using [scodec](http://scodec.org/).

On top of this, one or more clients can be implemented. The only one currently available out of the box is built using
[fs2](https://functional-streams-for-scala.github.io/fs2/)/[cats effect](https://typelevel.org/cats-effect/) but
more competing implementations can be added with limited effort. This implementation has found great inspiration from
the excellent [fs2-kafka](https://github.com/Spinoco/fs2-kafka/) library.

What's there:
- [x] Codecs for Redis' RESP wire format
- [x] Fully-fledged protocol encapsulating request/response pairs for (almost) all Redis [commands](https://redis.io/commands)
- [x] Initial version of single-node Redis client. Lots of improvements needed
- [x] Minimal CLI

What's missing:
- [ ] Everything else :)

**Note:** the library is still evolving and more features will be added in the future. Even if the binary compatibility will not be guaranteed until version 1.0.0, the [semantic versioning](https://semver.org/) strategy will be observed in the process.

### Why the name

Two reasons:
1. "A LaserDisc" is an anagram for "Scala Redis"
2. LaserDiscs were invented in 1978 (same year I was born) and were so cool (and foundational, more on [Wikipedia](https://en.wikipedia.org/wiki/LaserDisc))

### Getting Started

LaserDisc is currently available for Scala 2.12 and 2.13 on the JVM.

Its core (protocol commands and RESP wire format) is also available for [Scala.JS](http://www.scala-js.org/).

To add LaserDisc as a dependency to your project just add the following to your `build.sbt`:
```
libraryDependencies += "io.laserdisc" %% "laserdisc-fs2" % latestVersion
```

If you only need protocols (i.e. Redis commands and RESP wire format), you may simply add:
```
libraryDependencies += "io.laserdisc" %% "laserdisc-core" % latestVersion
```

### Interoperability modules

Support for existing libraries is available via dedicated dependencies.

#### [Circe](https://circe.github.io/circe/)

When an `io.circe.Decoder[A]` and a `io.circe.Encoder[A]` are implicilty available,
instances of `Show[A]` and `Read[Bulk, A]` can be derived for free,
just add the following in your `build.sbt`:

```
libraryDependecies += "io.laserdisc" %% "laserdisc-circe" % latestVersion 
```

then, to make use of them, at call site it should be sufficient to just:

```scala
import laserdisc.interop.circe._
```

*Note*: the derived `Show[A]` instance uses the most compact string representation
of the JSON data structure, i.e. no spacing is used

### Example usage
With a running Redis instance on `localhost:6379` try running the following:
```scala
import cats.effect.{IO, IOApp}
import log.effect.LogWriter
import log.effect.fs2.SyncLogWriter
import cats.syntax.flatMap._

object Main extends IOApp.Simple {

  import laserdisc._
  import laserdisc.all._
  import laserdisc.auto._
  import laserdisc.fs2._

  def redisTest(implicit log: LogWriter[IO]): IO[Unit] =
    RedisClient[IO].to("localhost", 6379).use { client =>
      client.send(
        set("a", 23),
        set("b", 55),
        get[PosInt]("b"),
        get[PosInt]("a")
      ) >>= {
        case (Right(OK), Right(OK), Right(Some(getOfb)), Right(Some(getOfa))) if getOfb.value == 55 && getOfa.value == 23 =>
          log info "yay!"
        case other =>
          log.error(s"something went terribly wrong $other") >>
            IO.raiseError(new RuntimeException("boom"))
      }
    }

  override final def run: IO[Unit] =
    redisTest(SyncLogWriter.consoleLog[IO])
}
```

This should produce an output similar to the following one:
```
[debug] - [io-compute-3] Starting connection
[info] - [io-compute-4] Connected to server localhost:6379
[trace] - [io-compute-4] sending Arr(Bulk(SET),Bulk(a),Bulk(23))
[trace] - [io-compute-3] receiving Str(OK)
[trace] - [io-compute-1] sending Arr(Bulk(SET),Bulk(b),Bulk(55))
[trace] - [io-compute-4] receiving Str(OK)
[trace] - [io-compute-0] sending Arr(Bulk(GET),Bulk(b))
[trace] - [io-compute-5] receiving Bulk(55)
[trace] - [io-compute-0] sending Arr(Bulk(GET),Bulk(a))
[trace] - [io-compute-0] receiving Bulk(23)
[info] - [io-compute-5] yay!
[debug] - [io-compute-5] Shutting down connection
[debug] - [io-compute-5] Shutdown complete
[info] - [io-compute-0] Connection terminated: No issues
```

## Dependencies

|      | Shapeless |
| ----:| ---------:|
| [![Maven Central](https://img.shields.io/maven-central/v/io.laserdisc/laserdisc-core_2.13.svg?label=laserdisc%20core&colorB=orange)](https://index.scala-lang.org/laserdisc-io/laserdisc/laserdisc-core) | 2.3.10 |

<br>

|      | Fs2 | Log Effect | Laserdisc Core |
| ----:| ---:| ----------:| --------------:|
| [![Maven Central](https://img.shields.io/maven-central/v/io.laserdisc/laserdisc-fs2_2.13.svg?label=laserdisc%20fs2&colorB=blue)](https://index.scala-lang.org/laserdisc-io/laserdisc/laserdisc-fs2) | 3.4.0 | 0.16.3 | [![Maven Central](https://img.shields.io/maven-central/v/io.laserdisc/laserdisc-core_2.13.svg?label=%20&colorB=orange)](https://index.scala-lang.org/laserdisc-io/laserdisc/laserdisc-core) |

<br>

|      | Circe | Laserdisc Core |
| ----:| -----:| --------------:|
| [![Maven Central](https://img.shields.io/maven-central/v/io.laserdisc/laserdisc-circe_2.13.svg?label=laserdisc%20circe&colorB=darkgreen)](https://index.scala-lang.org/laserdisc-io/laserdisc/laserdisc-circe) | 0.14.3 | [![Maven Central](https://img.shields.io/maven-central/v/io.laserdisc/laserdisc-core_2.13.svg?label=%20&colorB=orange)](https://index.scala-lang.org/laserdisc-io/laserdisc/laserdisc-core) |

## Support

![YourKit Image](https://www.yourkit.com/images/yklogo.png "YourKit")

This project is supported by YourKit with monitoring and profiling Tools. YourKit supports open source with innovative and intelligent tools for monitoring and profiling Java and .NET applications. YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/), [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/), and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).

## License

LaserDisc is licensed under the **[MIT License](LICENSE)** (the "License"); you may not use this software except in
compliance with the License.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and limitations under the License.
