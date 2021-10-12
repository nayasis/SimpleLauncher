# Simple Launcher

## Introduction

***Simple Launcher*** is designed to launch all of your application simply and to search easily.

It accepts executable command or relative path to run so you could customize your own application launcher independent with OS.

![screenshot](https://github.com/nayasis/simpleLauncher/blob/master/doc/screenshot.jpg?raw=true)

## Requirements

- Java 11 above
- JavaFX 13 above

## Execution
```shell
java -jar SimpleLauncher.jar clear
```
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

| shortcut       | description        |
| -------------- | ------------------ |
| Enter          | execute item       |
| Delete         | delete item        |

### Launcher editor

only works when Launcher editor is opened.

| shortcut       | description             |
| -------------- | ------------------      |
| Ctrl + **N**   | new item                |
| Ctrl + **D**   | delete item             |
| Ctrl + **C**   | copy item               |
| Ctrl + **S**   | save item               |
| Ctrl + **F**   | copy item's folder path |
| Ctrl + **O**   | open item's folder      |
| Ctrl + **I**   | change item icon        |

### Etc
| shortcut       | description                 |
| -------------- | ------------------          |
| Ctrl + Shift + **N** | create link via file  |

## Binding parameters

Item's ***option***(or prefix option) could have parameter and it would replace to file(or directory) path
being injected when runs by file(or directory) dragging.

| parameter      | description                 | example                |
|----------------|-----------------------------|------------------------|
| \#{path}       | file's full path            | \\usr\\path\\readme.md |
| \#{path-unix}  | file's full path as unix    | /usr/path/readme.md    |
| \#{dir}        | file's directory            | \\user\\path           |
| \#{dir-unix}   | file's directory as unix    | /user/path             |
| \#{file}       | file name                   | readme.md              |
| \#{name}       | file name without extension | readme                 |
| \#{ext}        | extension                   | md                     |
| \#{home}       | user home directory         | \\home\\***user***     |
| \#{home-unix}  | user home directory as unix | /home/***user***       |

### Example

ape2wav.exe "#{path}" "#{dir}\#{name}.wav"

## Contact

FAQs  : https://github.com/nayasis/simpleLauncher2/issues

email : [nayasis@gmail.com](mailto:nayasis@gmail.com)

## Development

### Gradle

This is **SpringBoot** application and run through **Spring Boot Loader**. 

#### Run from source
```shell
gradle bootRun
```

#### Build
```shell
gradle bootJar
```
JAR file will be created in <u>${Project Directory}**/build/libs**</u>  
It can be run like follow.
```shell
java -jar simplelauncher.jar
```



