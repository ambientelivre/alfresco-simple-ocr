```
$ docker build -t keensoft/pdfsandwich:1.6 .
$ alias pdfsandwich='docker run --rm -v `pwd`:/work -w /work keensoft/pdfsandwich:1.6'
$ pdfsandwich test.pdf
```
