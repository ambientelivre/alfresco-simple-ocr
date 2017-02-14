```
$ docker build -t keensoft/pdfsandwich:1.4 .
$ alias pdfsandwich='docker run --rm -v `pwd`:/work -w /work keensoft/pdfsandwich:1.4'
$ pdfsandwich test.pdf
```