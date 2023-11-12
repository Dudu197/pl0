(ns ifes.pl0.core
  (:require [instaparse.core :as insta]
            [clojure.core.match :refer [match]]))

(def whitespace-or-comment
  "Parser que se encarrega de identificar os espaços em branco e comentários do
  código fonte."
  (insta/parser (clojure.java.io/resource "pl0-ws.bnf")))

(def parser
  "Parser que reconhece o código fonte da linguagem PL/0. Note que o parser
  `whitespace-or-comment` é passado para a opção `:auto-whitespace`."
  (insta/parser (clojure.java.io/resource "pl0.bnf")
                :auto-whitespace whitespace-or-comment))

(defrecord Binding [kind value])

(defrecord Env [bindings parent])

(defn new-env
  "Cria um novo ambiente. Se não for especificado `parent`, é criado um ambiente
  raiz, caso contrário é criado um ambiente filho do ambiene indicado."
  ([] (->Env {} nil))
  ([parent] (->Env {} parent)))

(defn is-defined?
  "Retorna `true` se o `name` estiver definido em `env`, e `false` caso contrário.
  Testa o ambiente no nível atuale toda a sequência de `parent` até o nível
  raiz."
  [env name]
  (or (contains? (:bindings env) name)
      (and (some? (:parent env)) (is-defined? (:parent env) name))))


(defn def-name
  "Acrescenta uma definição do tipo (`kind`) indicado para `name` com o `value`
  indicado. Não valida se o valor é apropriado para o tipo informado."
  [env name kind value]
  (assoc-in env [:bindings name] (->Binding kind value)))

(defn def-var [env name] (def-name env name :var nil))
(defn def-const [env name value] (def-name env name :const value))
(defn def-proc [env name body] (def-name env name :proc body))


(defn set-var
  "Retorna um novo ambiente à partir de `env` com o valor `value` associado à
  variável `name`. Gera um erro se `name` não estiver definido ou se não for uma
  variável."
  [env name value]

  (cond
    (some? (get-in env [:bindings name]))
    (let [v (get-in env [:bindings name])]
      (match [(:kind v)]
        [:var] (assoc-in env [:bindings name :value] value)
        [:const] (throw (ex-info (str "Name is a constante: " name) v))
        [:proc] (throw (ex-info (str "Name is a procedure: " name) v))))
    (some? (:parent env)) (assoc env :parent (set-var (:parent env) name value))
    :else (throw (ex-info (str "Undefined variable: " name) {}))))


(defn get-value
  "Retorna o valor da variável ou constante `name` no ambiente `env`. Gera um
  erro se `name` não for definido, ou se não for uma variável ou uma constante."
  [env name]

  (cond
    (some? (get-in env [:bindings name]))
    (let [v (get-in env [:bindings name])]
      (match [(:kind v)]
        [:var] (:value v)
        [:const] (:value v)
        [:proc] (throw (ex-info (str "Name is a procedure: " name) v))))
    (some? (:parent env)) (recur (:parent env) name)
    :else (throw (ex-info (str "Undefined variable or constant: " name) {}))))


(defn get-proc
  "Retorna a definição do procedimento `name` no ambiente `env`. Gera um erro se
  `name` não for definido, ou se não for um procedimento."
  [env name]
  (cond
    (some? (get-in env [:bindings name]))
    (let [v (get-in env [:bindings name])]
      (match [(:kind v)]
        [:var] (throw (ex-info (str "Name is a variable: " name) v))
        [:const] (throw (ex-info (str "Name is a constante: " name) v))
        [:proc] (:value v)))
    (some? (:parent env)) (get-proc (:parent env) name)
    :else (throw (ex-info (str "Procedure not defined: " name) {}))))

(declare exec-block exec-decl exec-const-decl exec-var-decl
         exec-sttmt exec-progn exec-if exec-while
         eval-expr eval-cond eval-rel-expr)

(defn exec
  "Avalia o programa `prog` escrito em PL/0."
  [prog & {:keys [return-env] :or {return-env false}}]
  (let [env (-> prog
                (parser)
                (first)
                (exec-block))]
    (if return-env env nil)))

(defn exec-block
  "Avalia um bloco de código PL/0. Um bloco de código é uma sequência de
  declarações. A avaliação/execução de cada declaração pode gerar mudanças no
  ambiente de execução do programa."
  ([ast] (exec-block ast (new-env)))
  ([ast env]
   (match [ast]
     [[]] env
     [[:block & decls]] (recur decls env)
     [[d1 & ds]] (->> env
                      (exec-decl d1)
                      (recur ds)))))


(defn exec-decl
  "Avalia/executa uma declaração de um bloco. Uma declaração pode ser: (i)
  declaração de constantes; (ii) declaração de variáveis; (iii) definição de
  procedimento; ou (iv) um `statement`."
  [decl env]
  (match [decl]

    [[:const_decl & const-inits]] (exec-const-decl const-inits env)

    [[:var_decl & var-ids]] (exec-var-decl var-ids env)

    [[:proc_decl [:ident name] body]] (def-proc env name body)

    [[:statement & _]] (exec-sttmt decl env)))


(defn exec-var-decl
  "Atualiza o ambiente `env` com as definições das variáveis dadas em `idents`.
  Cria um novo ambiente, à partir de `env` em que as variáveis comos nomes dados
  em `idents` estão definidas. Retorna o novo ambiente."
  [idents env]
  (match [idents]
    [[]] env
    [[[:ident name] & idents1]]
    (->> (def-var env name)
         (recur idents1))))


(defn exec-const-decl
  "Atualiza o ambiente `env` com as definições das constantes dadas em `inits`.
  Cria um novo ambiente, à partir de `env` em que as constantes comos nomes e
  valores dados em `inits` estão definidas. Retorna o novo ambiente."
  [inits env]
  (match [inits]
    [[]] env
    [[[:ident name] "=" [:number num] & inits1]]
    (->> (Integer/parseInt num)
         (def-const env name)
         (recur inits1))))

(defn exec-sttmt
  "Executa um comando (statement) PL/0 e retorn o ambiente resultante."
  [sttmt env]

  (match [sttmt]

    [[:statement [:ident var] ":=" expr]]
    (let [value (eval-expr expr env)]
      (set-var env var value))

    [[:statement "call" [:ident name]]]
    (-> (get-proc env name)
        (exec-block (new-env env))
        (get :parent))

    [[:statement "?" [:ident name]]]
    (->> (do (printf "%s? " name)
             (flush)
             (read-line))
         (Integer/parseInt)
         (set-var env name))

    [[:statement "!" expr]]
    (do (->> (eval-expr expr env)
             (println))
        env)

    [[:statement "begin" & sttmt-seq]]
    (exec-progn sttmt-seq env)

    [[:statement "if" cond1 sttmt1]]
    (exec-if cond1 sttmt1 env)

    [[:statement "while" cond1 sttmt1]]
    (exec-while cond1 sttmt1 env)

    ))


(defn exec-progn
  "Executa os comandos em `sttmts` sequencialmente com o ambiente `env`. O
  primeiro comando é executado com `env`. O ambiente retornado pelo primeiro
  comando é usado para executar o segundo comando. O ambiente retornado pelo
  segundo comando é usado para executar o terceiro comando e assim por diante.
  Retorna o ambiente retornado pelo último comando."
  [sttmts env]

  (match [sttmts]
    [[]] env
    [[stt1 & stts]]
    (->> (exec-sttmt stt1 env)
         (recur stts))))


(defn exec-if
  "Avalia o valor de `cnd` com o ambiente `env`; se o valor for diferente de zero,
  executa `sttmt` usando `env` e retorna o ambiente resultante. Caso contrário
  retorna `env`."
  [cnd sttmt env]
  (let [cnd-val (eval-cond cnd env)]
    (if (zero? cnd-val)
      env
      (exec-sttmt sttmt env))))


(defn exec-while
  "Avalia `cnd` em `env`; se o valor for zero, retorna `env`; caso contrário,
  executa `sttmt` em `env` e usa o ambiente resultante para repetir o processo
  recursivamente."
  [cnd sttmt env]
  (let [cnd-val (eval-cond cnd env)]
    (if (zero? cnd-val)
      env
      (->> (exec-sttmt sttmt env)
           (recur cnd sttmt)))))

(declare eval-term eval-factor)

(defn eval-cond
  "Avalia a condição `cnd` de acrodo com as definições de `env` e retorna
  verdadeiro (1) ou falso (0). A condição pode ser `odd <expr>` ou `<expr> <op>
  <expr>`, onde `<op>` é um operador relacional."
  [cnd env]
  (match [cnd]
    [[:condition "odd" expr1]]
    (let [value (eval-expr expr1 env)]
      (mod value 2))
    [[:condition expr1 op expr2]]
    (eval-rel-expr expr1 op expr2 env)))


(defn eval-rel-expr
  "Avalia uma expressão relacional com duas subexpressões, `expr1` e `expr2`, e um operador `op`
  relacional. Retorna 1 se a expressão for verdadeira e 0 caso contrário."
  [expr1 op expr2 env]
  (let [val1 (eval-expr expr1 env)
        val2 (eval-expr expr2 env)]
    (match [op]
      ["="] (if (= val1 val2) 1 0)
      [">"] (if (> val1 val2) 1 0)
      ["<"] (if (< val1 val2) 1 0)
      [(:or "<>" "#")] (if (not= val1 val2) 1 0)
      [">="] (if (>= val1 val2) 1 0)
      ["<="] (if (<= val1 val2) 1 0)

      )))


(defn eval-expr
  "Avalia a empressão `expr` no ambiente de execução `env`. Retorna o valor da
  expressão."
  ([expr env] (eval-expr expr nil env))
  ([expr acc env]
  (match [expr]
    [[]] acc
    [[:expression t1 & ts]] (let [v1 (eval-term t1 env)]
                              (eval-expr ts v1 env))
    [["+" t1 & ts]] (let [v1 (eval-term t1 env)]
                      (eval-expr ts (+ acc v1) env))
    [["-" t1 & ts]] (let [v1 (eval-term t1 env)]
                      (eval-expr ts (- acc v1) env)))))


(defn eval-term
  "Avalia o termo `term` no ambiente de execução `env`. Retorna o valor do termo."
  ([term env] (eval-term term nil env))
  ([term acc env]
   (match [term]
     [[]] acc
     [[:term f1 & fs]] (let [v1 (eval-factor f1 env)]
                         (eval-term fs v1 env))
     [["*" f1 & fs]] (let [v1 (eval-factor f1 env)]
                       (eval-term fs (* acc v1) env))
     [["/" f1 & fs]] (let [v1 (eval-factor f1 env)]
                       (eval-term fs (quot acc v1) env)))))


(defn eval-factor
  "Avalia o fator `factor` no ambiente de execução `env`. Retorna o valor do
  fator."
  [factor env]

  (match [factor]
    [[:factor "+" f1]] (let [v1 (eval-factor f1 env)] v1)
    [[:factor "-" f1]] (let [v1 (eval-factor f1 env)] (- v1))
    [[:factor [:ident name]]] (get-value env name)
    [[:factor [:number n]]] (Integer/parseInt n)
    [[:factor [:expression & _]]] (eval-expr (nth factor 1) env)

    ))
