# gif.kt

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

### Gradle

```kotlin
repositories {
    mavenCentral()

    // Uncomment the next line to use snapshot releases
    // maven("https://central.sonatype.com/repository/maven-snapshots")
}

dependencies {
    // Replace $version with the latest version
    implementation("com.shakster:gifkt:$version")
}
```

### Maven

```xml
<dependency>
    <groupId>com.shakster</groupId>
    <artifactId>gifkt</artifactId>
    <!-- Replace $version with the latest version -->
    <version>$version</version>
</dependency>
```

Requires JDK 8 or higher when targeting the JVM.

## Usage

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
// Obtain a Sink to write the GIF data to
val sink: Sink = ...
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
// Obtain an OutputStream to write the GIF data to
OutputStream outputStream = ...;
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

#### Kotlin

```kotlin
// Obtain a Sink to write the GIF data to
val sink: Sink = ...
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
encoder.writeFrame(argb1, width1, height1, duration1)
encoder.writeFrame(argb2, width2, height2, duration2)

encoder.close()
```

#### Java

```java
// Obtain an OutputStream to write the GIF data to
OutputStream outputStream = ...;
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
encoder.writeFrameFuture(argb1, width1, height1, duration1).join();
encoder.writeFrameFuture(argb2, width2, height2, duration2).join();

encoder.closeFuture().join();
```

## Modules

### Core

The [core](https://github.com/shaksternano/gif.kt/tree/main/core) module contains the main library code.

### CLI

The [cli](https://github.com/shaksternano/gif.kt/tree/main/cli) module contains a command-line interface GIF encoder
tool that uses the library to create GIFs from images, videos, and other GIFs.
