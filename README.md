# gif.kt

[![GitHub license](https://img.shields.io/github/license/shaksternano/gif.kt)](LICENSE)
[![Download](https://img.shields.io/maven-central/v/com.shakster/gifkt)](https://central.sonatype.com/artifact/com.shakster/gifkt)
[![Kotlin](https://img.shields.io/badge/kotlin-2.3.10-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Java](https://img.shields.io/badge/java-8-blue.svg?logo=openjdk)](https://www.java.com)
[![KDoc link](https://img.shields.io/badge/API_reference-KDoc-blue)](https://gifkt.shakster.com)

A Kotlin Multiplatform GIF decoding and encoding library.

## Features

- Supports static and animated GIFs.
- Easy to use API.
- Memory-efficient decoding.
- Fast parallel encoding.
- Automatic lossless compression when encoding, as well as optional lossy compression.
- Automatic handling of GIF minimum frame duration when encoding.
- Supports several GIF options, such as looping and comments.

## Installation

Requires JDK 8 or higher when targeting the JVM.

### Gradle

```kotlin
repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("com.shakster:gifkt:0.3.1")
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>google</id>
        <url>https://maven.google.com</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.shakster</groupId>
        <artifactId>gifkt-jvm</artifactId>
        <version>0.3.1</version>
    </dependency>
</dependencies>
```

## Usage

API documentation is available [here](https://gifkt.shakster.com).

### Decoding

#### Kotlin

```kotlin
// Obtain a Path to read the GIF data from
val path: Path = ...
val data = path.asRandomAccess()
val decoder = GifDecoder(data)

// Read a single frame by index
val frame1 = decoder[0]

// Read a single frame by timestamp
val frame2 = decoder[2.seconds]

// Read all frames
decoder.asSequence().forEach { frame ->
    // Process each frame
}

decoder.close()
```

#### Java

```java
// Obtain a Path to read the GIF data from
Path path = ...;
RandomAccessData data = RandomAccessData.of(path);
GifDecoder decoder = new GifDecoder(data);

// Read a single frame by index
ImageFrame frame1 = decoder.get(0);

// Read a single frame by timestamp
ImageFrame frame2 = decoder.get(Duration.ofSeconds(2));

// Read all frames
for (ImageFrame frame : decoder.asList()) {
    // Process each frame
}

decoder.close();
```

### Encoding

#### Kotlin

```kotlin
// Obtain a Path to write the GIF data to
val path: Path = ...
val sink = SystemFileSystem.sink(path).buffered()
val encoder = GifEncoder(sink)

val argb: IntArray = ...
val width: Int = ...
val height: Int = ...
val duration: Duration = ...
encoder.writeFrame(argb, width, height, duration)

encoder.close()
```

#### Java

```java
// Obtain a Path to write the GIF data to
Path path = ...;
OutputStream outputStream = Files.newOutputStream(path);
GifEncoderBuilder builder = GifEncoder.builder(outputStream);
GifEncoder encoder = builder.build();

int[] argb = ...;
int width = ...;
int height = ...;
Duration duration = ...;
encoder.writeFrame(argb, width, height, duration);

encoder.close();
```

### Parallel Encoding

Not supported on JavaScript or Wasm.

#### Kotlin

```kotlin
// Obtain a Path to write the GIF data to
val path: Path = ...
val sink = SystemFileSystem.sink(path).buffered()
// Use all available CPU cores for maximum encoding speed
val cpuCount: Int = ...
val encoder = ParallelGifEncoder(
    sink,
    maxConcurrency = cpuCount,
    ioContext = Dispatchers.IO,
)

val argb1: IntArray = ...
val width1: Int = ...
val height1: Int = ...
val duration1: Duration = ...

val argb2: IntArray = ...
val width2: Int = ...
val height2: Int = ...
val duration2: Duration = ...

// Frames are encoded in parallel
// Suspending
encoder.writeFrame(argb1, width1, height1, duration1)
encoder.writeFrame(argb2, width2, height2, duration2)

// Suspending
encoder.close()
```

#### Java

```java
// Obtain a Path to write the GIF data to
Path path = ...;
OutputStream outputStream = Files.newOutputStream(path);
// Use all available CPU cores for maximum encoding speed
int cpuCount = ...;
GifEncoderBuilder builder = GifEncoder.builder(outputStream);
builder.setMaxConcurrency(cpuCount);
builder.setIoContext(Dispatchers.getIO());
ParallelGifEncoder encoder = builder.buildParallel();

int[] argb1 = ...;
int width1 = ...;
int height1 = ...;
Duration duration1 = ...;

int[] argb2 = ...;
int width2 = ...;
int height2 = ...;
Duration duration2 = ...;

// Frames are encoded in parallel
encoder.writeFrameFuture(argb1, width1, height1, duration1)
    .thenCompose(unused -> encoder.writeFrameFuture(argb2, width2, height2, duration2))
    .thenCompose(unused -> encoder.closeFuture())
    .join();
```

## Modules

### Core

The [core](core) module contains the main library code.

### Compose

The [compose](compose) module contains [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform) integration.

Requires JDK 11 or higher when targeting the JVM.

#### Gradle

```kotlin
dependencies {
    implementation("com.shakster:gifkt-compose:0.3.1")
}
```

### CLI

The [cli](cli) module contains a command-line interface GIF encoder tool that uses the library to create GIFs from
images, videos, and other GIFs.

Requires JDK 21 or higher to run.

## Developing

JDK 21 or higher is required to build the project.

Run `./gradlew build` to build the project.

Run `./gradlew cleanAllTests allTests` to run all tests.

Run `./gradlew dokkaGenerate` to generate API documentation. The output will be in `core/build/dokka/html`.

## Snapshot Releases

A snapshot release is published with every push to the main branch.
To use a snapshot release, add the Maven snapshot repository and append `-SNAPSHOT` to the version.

### Gradle

```kotlin
repositories {
    mavenCentral()
    google()
    maven("https://central.sonatype.com/repository/maven-snapshots")
}

dependencies {
    implementation("com.shakster:gifkt:0.3.1-SNAPSHOT")
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>google</id>
        <url>https://maven.google.com</url>
    </repository>
    <repository>
        <id>maven-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.shakster</groupId>
        <artifactId>gifkt-jvm</artifactId>
        <version>0.3.1-SNAPSHOT</version>
    </dependency>
</dependencies>
```
