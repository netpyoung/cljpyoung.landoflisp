npm init -y
npm install shadow-cljs
npm install react react-dom
npm install process

npx shadow-cljs watch app


deps.edn
shadow-cljs.edn



React 버전 Reagent 버전
17     1.1.x 이하
18     1.2.0 이상
19     2.0.0

- https://github.com/reagent-project/reagent
- https://react.dev/versions
- https://factorhouse.io/blog/articles/beyond-reagent-with-hsx-and-rfx/


rimraf - https://github.com/isaacs/rimraf


svg - g - group



- <https://github.com/robjens/boot-reframe-10x>
- <https://github.com/Day8/re-frame-10x/issues/196>
- <https://clojars.org/cljsjs/create-react-class>
- <https://www.youtube.com/watch?v=JCY_cHzklRs>

![](./landoflisp.svg)


- TODO: <https://github.com/Day8/re-com>
* ch05 - https://github.com/netpyoung/cljpyoung.spels/blob/master/src/cljpyoung/spels.clj

## Ref.

- <http://landoflisp.com/source.html>
- <https://github.com/quux00/land-of-lisp-in-clojure/blob/master/grand-theft-wumpus/src/thornydev/wumpus/game.clj>
- <https://github.com/bitsai/book-exercises/blob/master/Land%20of%20Lisp%20(in%20Clojure)/ch8/wumpus.clj>
- <http://derekmcloughlin.github.io/2014/09/13/Haskell-Dice-Of-Doom-Part-1/>
- <http://derekmcloughlin.github.io/2014/10/04/Haskell-Dice-Of-Doom-Part-2/>
- <http://derekmcloughlin.github.io/2014/11/02/Haskell-Dice-Of-Doom-Part-3/>

## 챕터

### ch15

2x2

``` txt
player: int
dice : int

tree {
      :player player
      :board [[player dice]]
      :moves [move ...]
     }

move {
      :action ([from to] | nil)
      :tree tree
     }
```

게임이서 나올 수 있는 경우의 수를 모두 계산한다.
Game Tree
2x2에서는 충분히 가능하나
3x3부터는 계산하는데 많은 시간이 걸린다.

### 16 - macro
### 17 - svg lib

### 18

- lazy evaluation
  - 지연평가로 모두 다 계산하는게 아니라 필요한 만큼만 계산하도록 바꾼다.
  - context만 들고있고 평가는 미루는 것이다.

- trimming
- Heuristics


- <http://popungpopung.tistory.com/10>
- [A simple animation of the Minimax algorithm](https://www.youtube.com/watch?v=zDskcx8FStA)
- [Step by Step: Alpha Beta Pruning](https://www.youtube.com/watch?v=xBXHtz4Gbdo)

aI도 들어간다. 몇수 앞을 미리 계산하여 최적의 선택을 하도록 한다.

#### minimax 알고리즘

- <https://en.wikipedia.org/wiki/Minimax>

- 상대편에게는 이길 수 있는 확율이 적은 min
- 나에게는 이길 수 있는 확율이 높은 max
- 한계점
  - 모두 돌아봐야함


#### alpha-beta prune

- alpha cut-off 는 자신이 상대방보다 불리하여, 자신이 그 경우를 선택하지 않을 때 불필요한 연산을 잘라내는 것이고
- Beta cut-off 는 자신이 상대방보다 유리하여, 상대방이 그 경우를 선택하지 않을 확률이 높을 때 불필요한 연산을 잘라내는 것이다. 둘의 차이점을 잘 알아두자

### 19장

svg / webserver

### 20장

- AI는 이미 4인용 게임에 적절
- 6각 tile에 대한 주사위의 최대 갯수를 3에서 5로 늘렸으며, AI 레벨을 4에서 2로 줄였다
