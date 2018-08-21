# SFT Learning Algorithm
I aim to learn models of sanitizers (by using automata learning algorithms) in a black-box manner, so that I can reason about the correctness of sanitizers. 
The code in this repository is based on [symbolicautomata](https://github.com/lorisdanto/symbolicautomata), which is a library for automata. 
This repository contains implementations for:
* SFA (Symbolic Finite Automata) learning algorithm
* SFT (Symbolic Finite Transducer) learning algorithm

These algorithms can ask a sanitizer two types of questions: a membership query or an equivalence query. Based on the results, it will then derive a model (SFA/SFT).


## Membership Oracle
The membership oracle is used to pose membership queries. These queries give input to some sanitizer and then observe the output of the sanitizer. 
To be able to pose membership queries, we require a _**command**_ from the user.
This command may contain options/flags.
The program should call the sanitizer and pass it the commandline argument which is the input to the sanitizer.
The input to the sanitizer will be appended to your command by the algorithm.
If the command is run, it should print the output of the program to the _**standard output**_
Note that this program may need to be compiled beforehand, depending on the programming language which was used.
We recommend trying to execute the command beforehand from the command line and using the *__full path__* to reference a specific program.
Note that any libraries, languages or compilers that are used by the sanitizer should be installed on the computer on which SFTLearning is run.

Here are some examples of commands (all the programs mentioned below can be found in [here](https://github.com/Sophietje/SFTLearning/tree/master/Sanitizers/src)):
- ```./escapeHTML```
- ```java EscapeHTML```
- ```node escapeHTML.js```
- ```php escapeHTML.php```
- ```python escapeHTML.py```
- ```ruby escapeHTML.rb```


## Equivalence Oracle
The equivalence oracle is used to pose equivalence queries. These queries try to discover whether the hypothesis automaton is a correct model of the sanitizer.
If the equivalence oracle does not provide correct counterexamples, then the algorithm will be unable to learn the correct model of the program.
The equivalence oracle can be implemented in many different ways. The following have already been implemented in SFTLearning:
* Random testing
* Random prefix selection
* State coverage
* Transition (or branch) coverage
* Predicate (or condition) coverage
* History-based 

After choosing a specific oracle, the user will be asked to provide some parameters such as number of tests in total or number of tests per state.


## Specifications
This research aimed to be able to reason about the correctness of sanitizers by comparing a derived model to a specification. This specification needs to be written by the user in the form of an SFA or SFT.
It should be provided in a [DOT](https://en.wikipedia.org/wiki/DOT_(graph_description_language)) file with a specific structure.
The program will then ask which type of specification the user wants to check.
The following types of specifications can be checked:
* **Equality**
* **Blacklist (input/output)**
* **Whitelist (input/output, equal/subset)**
* **Length (input/output, equal/unequal)**
* **Idempotency**
* **Commutativity**
* **Bad output**

##### Structure of the specification
The specifications should be written in a DOT file which adheres to the following structure:
* **State declaration**: ```number[label=x, peripheries=y]``` where ```number``` denotes the state's number, ```x``` should be replaced by the label and ```y``` should be replaced by the number of peripheries (2 if it is a final state or 1 if it is not a final state).
* **Initial state declaration**: ```XXnumber [attributes]XXnumber -> number``` where ```number``` should be replaced by the state's number and ```attributes``` can be replaced by some attributes that the user wants to define. 
* **Transition declaration**: ```fromState -> toState [label="[x]/y``` where ```fromState``` should be replaced by the state where the transition starts, ```toState``` should be replaced by the state  where the transition leads to, ```x``` should be replaced by the guard and ```y``` should be replaced by the term functions. Guards should be denoted in unicode references "\u0000". Multiple guards should not be separated by anything. The range ```a``` to ```b``` can be denoted as a guard using ```a-b```. Terms should be written as "x+0" for the identity function, or "y" where y is the specific constant which is outputted. Multiple term functions should be separated with a whitespace.

An example of a specification which defines a sanitizer that represents [CyberChef's](https://github.com/gchq/CyberChef) "Remove whitespace" function:
```
digraph spec{
 rankdir=LR;
0[label=0,peripheries=2]
XX0 [color=white, label=""]XX0 -> 0
0 -> 0 [label="[\r]/"]
0 -> 0 [label="[.]/"]
0 -> 0 [label="[\t]/"]
0 -> 0 [label="[\n]/"]
0 -> 0 [label="[ ]/"]
0 -> 0 [label="[\f]/"]
0 -> 0 [label="[\u0000-\b\u000b\u000e-\u001f!-\-/-\uffff]/x+0"]
}
```



## Running the program
To derive a model from a sanitizer, execute the class [TestAutomaticOracles.java](https://github.com/Sophietje/SFTLearning/blob/master/SVPAlib/src/sftlearning/TestAutomaticOracles.java).
This program will prompt the user for any necessary information via the standard input.
Make sure to adapt the path in this file (at the end of the main method) so that the learned model is stored in a DOT file.
If you want to check specifications, then call the desired methods in [SpecificationChecking.java](https://github.com/Sophietje/SFTLearning/blob/master/SVPAlib/src/sftlearning/SpecificationChecking.java).
If you want to learn a model **and** check a specification, then execute the class [CompareToSpec](https://github.com/Sophietje/SFTLearning/blob/master/SVPAlib/src/sftlearning/CompareToSpec.java).
