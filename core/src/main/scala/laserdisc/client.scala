/*
 * Copyright (c) 2018-2025 LaserDisc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package laserdisc

import shapeless.ops.hlist.ToSized
import shapeless.ops.sized.ToHList
import shapeless._

import scala.annotation.implicitNotFound
import scala.collection.LinearSeq
import scala.concurrent.duration._

trait ClientBase[F[_], Env] {
  def defaultTimeout: FiniteDuration = 20.seconds

  def send[In <: HList, Out <: HList](in: In, timeout: FiniteDuration)(implicit handler: Handler.Aux[F, Env, In, Out]): F[Out]
  final def send[In <: HList, Out <: HList](in: In)(implicit ev: Handler.Aux[F, Env, In, Out]): F[Out] = send(in, defaultTimeout)

  final def send[A1](protocolA1: Protocol.Aux[A1], timeout: FiniteDuration)(
      implicit F: Functor[F],
      ev: Handler.Aux[F, Env, Protocol.Aux[A1] :: HNil, Maybe[A1] :: HNil]
  ): F[Maybe[A1]] = F.map(send(protocolA1 :: HNil, timeout))(_.head)
  final def send[A1](protocolA1: Protocol.Aux[A1])(
      implicit F: Functor[F],
      ev: Handler.Aux[F, Env, Protocol.Aux[A1] :: HNil, Maybe[A1] :: HNil]
  ): F[Maybe[A1]] = send(protocolA1, defaultTimeout)

  final def send[CC[x] <: LinearSeq[x], A, N <: Nat, In <: HList, Out <: HList](
      sizedSeq: Sized[CC[Protocol.Aux[A]], N],
      timeout: FiniteDuration
  )(
      implicit F: Functor[F],
      toHList: ToHList.Aux[CC[Protocol.Aux[A]], N, In],
      ev0: Handler.Aux[F, Env, In, Out],
      ev1: ToSized.Aux[Out, CC, Maybe[A], N]
  ): F[Sized[CC[Maybe[A]], N]] = F.map(send(toHList(sizedSeq), timeout))(_.toSized)
  final def send[CC[x] <: LinearSeq[x], A, N <: Nat, In <: HList, Out <: HList](sizedSeq: Sized[CC[Protocol.Aux[A]], N])(
      implicit F: Functor[F],
      toHList: ToHList.Aux[CC[Protocol.Aux[A]], N, In],
      ev0: Handler.Aux[F, Env, In, Out],
      ev1: ToSized.Aux[Out, CC, Maybe[A], N]
  ): F[Sized[CC[Maybe[A]], N]] = send(sizedSeq, defaultTimeout)
}

trait Client[F[_], Env] extends ClientBase[F, Env] with ClientExt[F, Env]

trait Handler[F[_], Env, In <: HList] extends DepFn2[Env, In] {
  override final type Out = F[LOut]
  type LOut <: HList
}

object Handler {
  @implicitNotFound(
    """Cannot derive Handler[${F}, ${Env}, ${In}] { type Out = ${LOut0} }

This could depend on many things but most likely:
  - ${In} is not an HList of only laserdisc.Protocol types
  - deriving this Handler requires other type classes to be available in implicit scope

Try running scalac with -Xlog-implicits (or https://github.com/tek/splain)
"""
  ) type Aux[F[_], Env, In <: HList, LOut0 <: HList] = Handler[F, Env, In] { type LOut = LOut0 }
}
