# Simple Launcher

## Introduction

***Simple Launcher*** is designed to launch all of your application simply and to search easily.  
You are free to use **relative path** or **configurable command** to manage your own application launch commands.

![screenshot](https://github.com/nayasis/simpleLauncher/blob/master/doc/screenshot.jpg?raw=true)

## Shortcuts

### Menu

| shortcut             | description                   |
| --------------       | ------------------            |
| Ctrl + Shift + **I** | import application catalog    |
| Ctrl + Shift + **X** | export application catalog    |
| Ctrl + Shift + **D** | clear  application catalog    |
| ALT + **E**          | toggle detail launcher editor |
| ALT + **V**          | toggle menu bar               |
| ALT + **G**          | toggle group filter           |
| Ctrl + Shift + **F** | set windows on top always     |
| **F1**               | show help                     |

### Main catalog

| shortcut       | description             |
| -------------- | ------------------      |
| Enter          | execute item            |
| Delete         | delete item             |
| Ctrl + **C**   | copy item's folder path |

### Link editor

Only works when Link editor is opened.

| shortcut             | description             |
| --------------       | ------------------      |
| Ctrl + **N**         | new item                |
| Ctrl + Shift + **N** | create link via file    |
| Shift + **DEL**      | delete item             |
| Ctrl + **D**         | duplicate item          |
| Ctrl + **S**         | save item               |
| Ctrl + Shift + **C** | copy item's folder path |
| Ctrl + **O**         | open item's folder      |
| Ctrl + **I**         | change item icon        |

![link editor](https://github.com/nayasis/simpleLauncher/blob/master/doc/link-editor.jpg?raw=true)

### Keyword history

When keyword focused

![keyword focused](https://github.com/nayasis/simpleLauncher/blob/master/doc/keyword-focused.jpg?raw=true)

Enter history mode by pressing ***Alt + Down***.

![history mode](https://github.com/nayasis/simpleLauncher/blob/master/doc/keyword-historymode.jpg?raw=true)

#### keyword history functions

| shortcut         | description             |
| --------------   | ------------------      |
| Alt + **Left**   | previous link executed  |
| Alt + **Right**  | next link executed      |
| any key          | filter executed links ![autocompleted links](https://github.com/nayasis/simpleLauncher/blob/master/doc/keyword-autocomplete.jpg?raw=true)  |
| Esc              | exit history mode       |

## Binding parameters

Item's ***option***(or prefix option) could have parameter and it would replace to file(or directory) path
being injected when runs by file(or directory) dragging.

| parameter      | description                 | example                |
|----------------|-----------------------------|------------------------|
| \${path}       | file's full path            | \\usr\\path\\readme.md |
| \${path-unix}  | file's full path as unix    | /usr/path/readme.md    |
| \${dir}        | file's directory            | \\user\\path           |
| \${dir-unix}   | file's directory as unix    | /user/path             |
| \${file}       | file name                   | readme.md              |
| \${name}       | file name without extension | readme                 |
| \${ext}        | extension                   | md                     |
| \${home}       | user home directory         | \\home\\***user***     |
| \${home-unix}  | user home directory as unix | /home/***user***       |

### Example

ape2wav.exe "${path}" "${dir}\\${name}.wav"

## Development

This is **SpringBoot** application and run through **Spring Boot Loader**.

#### 1. equirements
- Java 11
- JavaFx 19

#### 2. Download source
```shell
git clone https://github.com/nayasis/SimpleLauncher.git
```

#### 3. Build
```
gradlew runtime
```
- You could also launch application directly.
  ```shell
  gradlew bootRun
  ```

#### 4. Execution
- Application would be compiled in directory [./build/image] including JRE.
- Launch application like this.
```shell
cd ./build/image/bin
simplelauncher
```

#### 5. For Windows
- Provide EXE application on [release page](https://github.com/nayasis/SimpleLauncher/releases).
  - Wrapped by [WinRun4J](https://winrun4j.sourceforge.net)

#### 6. Run from source
```shell
gradlew bootRun
```

## Contact

- issue : https://github.com/nayasis/SimpleLauncher/issues
- email : [nayasis@gmail.com](mailto:nayasis@gmail.com)
