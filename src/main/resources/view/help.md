# Simple Launcher

## Introduction

## Command Arguments

## Binding parameters

Item's ***option***(or prefix option) could have parameter and it would replace to file(or directory) path parameter
being injected when runs by file(or directory) dragging.

| parameter | description       | example      |
|-----------|-------------------|--------------|
| \#{cd}    | path only         | /user/home   |
| \#{name}  | name only         | readme.md    |
| \#{pname} | name only without extention | readme |
| \#{path}  | file's full path            | /usr/home/readme.md |

1. ParameterKey is case-insensitive. ( #{cd}, #{Cd}.. anything possible)
2. Example
   ape2wav.exe \"#{path}\" \"#{unextpath}\"  

## Contact

nayasis@gmail.com


