# Simple Launcher

## Introduction

***Simple Launcher*** is designed to launch all of your application simply and to search easily.

## Command Arguments


## Shortcuts

| shortcut     | description      |
|--------------|------------------|
| \#{filepath} | file's full path |
| \#{path}     | path             |
| \#{filename} | file name        |
| \#{name}     | base name        |
| \#{ext}      | extension        |

## Binding parameters

Item's ***option***(or prefix option) could have parameter and it would replace to file(or directory) path
being injected when runs by file(or directory) dragging.

| parameter    | description      | example             |
|--------------|------------------|---------------------|
| \#{filepath} | file's full path | /usr/home/readme.md |
| \#{path}     | path             | /user/home          |
| \#{filename} | file name        | readme.md           |
| \#{name}     | base name        | readme              |
| \#{ext}      | extension        | md                  |

### Example

   ape2wav.exe "#{filepath}" "#{path}\#{name}.wav"  

## Contact

FAQs  : https://github.com/nayasis/SimpleLauncher2/issues

email : [nayasis@gmail.com](mailto:nayasis@gmail.com)
