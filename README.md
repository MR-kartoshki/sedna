# Sedna RISC-V Emulator

Sedna is a 64-bit RISC-V emulator written purely in Java. It implements every extension needed to be considered "general purpose" and supports supervisor mode, so it can boot Linux. It passes the full [RISC-V test suite](https://github.com/riscv/riscv-tests), and it can serialize and deserialize machine state.

This is a fork of [fnuecke/sedna](https://github.com/fnuecke/sedna) modernized for Java 25 and Gradle 9. Sedna itself remains a pure library. The boot harness, kernel bundle and terminal front end that used to ship with upstream have moved into consumer projects like [linux-tui-mod](../linux-tui-mod).

## Fork state

Changes relative to upstream:

- Java toolchain 17 to 25. Foojay auto-provisions JDK 25 via the Gradle wrapper, so any JDK 17+ launcher can bootstrap the build without a manual JDK install.
- Gradle wrapper 9.6.1.
- ASM 9.1 to 9.8. Required because Sedna generates its instruction decoder at runtime by reading the compiled `R5CPUTemplate.class`, and Java 25 emits class file major version 69 which older ASM cannot parse.
- Mockito 5.20 and JUnit Jupiter 5.11, with the explicit `junit-platform-launcher` dependency that Gradle 9 now requires stated.
- `maven-publish` and the GitHub Packages upload block removed. No autopublish, and no GitHub Packages token needed to consume ceres.
- `org.gradle.jvmargs=--enable-native-access=ALL-UNNAMED` added to silence the JDK 25 restricted-method warning during the build.
- All 457 RISC-V ISA tests still pass on Java 25.

## Building

Requirements:

- A JDK for running Gradle. Any recent LTS such as JDK 21 works.
- A local checkout of [ceres](https://github.com/fnuecke/ceres) as a sibling directory next to this repo. `settings.gradle.kts` detects it and includes it as a composite build, which avoids needing GitHub Packages credentials to fetch the ceres artifact.

To build and run the tests:

```
./gradlew build
```

Sedna is a library. There is no standalone entry point in this repo. To boot Linux inside the emulator, use a consumer project such as the sibling `linux-tui-mod`, which wires in the boot harness, the kernel and root filesystem bundle, virtio-9p sharing, and a terminal front end.

## Structure

The code layout is relatively flat, with different parts of the emulator living in their own packages. The notable ones:

| Package                                                            | Description                                                 |
|--------------------------------------------------------------------|-------------------------------------------------------------|
| [li.cil.sedna.device](src/main/java/li/cil/sedna/device)           | Non-ISA specific device implementations.                    |
| [li.cil.sedna.devicetree](src/main/java/li/cil/sedna/devicetree)   | Utilities for constructing device trees.                    |
| [li.cil.sedna.elf](src/main/java/li/cil/sedna/elf)                 | An ELF loader, currently only used to load tests.           |
| [li.cil.sedna.fs](src/main/java/li/cil/sedna/fs)                   | Virtual file system layer for the VirtIO filesystem device. |
| [li.cil.sedna.instruction](src/main/java/li/cil/sedna/instruction) | Instruction loader and decoder generator.                   |
| [li.cil.sedna.memory](src/main/java/li/cil/sedna/memory)           | Memory map implementation and utilities.                    |
| [li.cil.sedna.riscv](src/main/java/li/cil/sedna/riscv)             | RISC-V CPU and devices (CLINT, PLIC).                       |

## RISC-V Extensions

Sedna implements the `G` meta extension, meaning the general purpose computing set of extensions `rv64imacfd` plus `Zifencei`. For the uninitiated:

- `i`: basic 64-bit integer ISA.
- `m`: integer multiplication, division, etc.
- `a`: atomic operations.
- `c`: compressed instructions.
- `f`: single precision (32-bit) floating-point operations.
- `d`: double precision (64-bit) floating-point operations.
- `Zifencei`: memory fence for instruction fetch.

Two caveats worth knowing:

- The `FENCE` and `FENCE.I` instructions are no-ops, and atomic operations do not lock underlying memory. Multi-core setups will behave incorrectly.
- Floating-point operations are implemented in software for flag correctness, which means they are slow.

## Instructions and decoding

Sedna uses run-time bytecode generation to build the decoder switch used by the instruction interpreter. This makes it easy to add new instructions and to experiment with different switch layouts for performance. The instruction loader and switch generator are technically general purpose and do not depend on the RISC-V part of the project, though some assumptions about how instructions are defined and processed are baked into the design.

The set of supported RISC-V instructions is declared [in this file](src/main/resources/riscv/instructions.txt). The implementations live in [the RISC-V CPU template class](src/main/java/li/cil/sedna/riscv/R5CPUTemplate.java).

## Endianness

The emulator presents itself as a little-endian system to code running inside it. It should also work correctly on big-endian hosts, though this has not been tested.

## Tests

Sedna tests ISA conformity using the [RISC-V test suite](https://github.com/riscv/riscv-tests). The tests run through a small JUnit [test runner](src/test/java/li/cil/sedna/riscv/ISATests.java), and the compiled test binaries live [here](src/test/data/riscv-tests).

Additional tests may come from this fork: https://github.com/fnuecke/riscv-tests

- A test for page-misaligned access (for example loads that span multiple pages) was contributed by @ja2142 on the [page_misaligned_access_test](https://github.com/fnuecke/riscv-tests/tree/page_misaligned_access_test) branch.

## License

Sedna is MIT licensed. See [LICENSE](LICENSE).
