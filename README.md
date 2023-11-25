# pl0

Implementação de um interpretador em PL0.

Exercício da disciplina Teoria da Computação do Instituto Federal de Educação, Ciência e Tecnologia do Espírito Santo.

O código base foi fornecido pelo professor, enquanto as implementações abaixo foram desenvolvidas pelo aluno.

## Novas implementações

### Parâmetros em métodos/procedures

Foi desenvolvido o uso de parâmetros nos métodos/procedures.
A passagem do parâmetro é dada por valor, não por referência.

O exemplo [Greater](https://github.com/Dudu197/pl0/blob/main/pl0-sources/greater.pl0) demonstra a passagem de parâmetros.

```
procedure greater(a, b);
var r;
begin
  if a > b then !a + " é maior que " + b;
  if b > a then !b + " é maior que " + a
end;
```


Para manter a compatibilidade com os códigos originais, a passagem do parâmetro foi feita de forma opcional.
Tanto os parâmetros quanto os parênteses (quando não há parâmetros).

```
procedure multiply;
var a, b;
begin
  a := x;
  b := y;
  z := 0;
  while b > 0 do
  begin
    if odd b then z := z + a;
    a := 2 * a;
    b := b / 2
  end
end;
```

### String

Outra nova implementação desenvolvida foi o tipo String.

Para o fazer o uso de String é necessário utilizar sempre aspas duplas (também quando lendo um valor via console).

O exemplo [String](https://github.com/Dudu197/pl0/blob/main/pl0-sources/string.pl0) demonstra o uso de strings.

```
(* -*- mode:pascal; -*-
 * Exemplo de programa em PL/0.
 * Lê um nome como String e escreve o nome no stdout.
 *)

var intro, nome;
begin
    intro := "Entre com o seu nome:";
    !intro;
    !"Favor colocar entre aspas duplas";
    ?nome;
    !"Seu nome é " + nome
end.
```


Também foi implementado a concatenação de strings. Ela é feita ao somar qualquer valor com uma string.


```
var a, b;
a := "string";
b := "Exemplo de concatenação de " + a.
```



## Installation

Download from http://example.com/FIXME.

## Usage

FIXME: explanation

    $ java -jar pl0-0.1.0-standalone.jar [args]

## Options

FIXME: listing of options this app accepts.

## Examples

...

### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2023 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
