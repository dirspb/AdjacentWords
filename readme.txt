Usage:
1. Put text file "test.txt" in the "data" subfoler.
2. Run the AdjacentWords.exe
3. Loading time depends on CPU performance and could take about 1 minute per 275 Mb of text
4. When it finally opens you can start using UI
5. You may move words by grabbing them by mouse (press left mouse button to drag)
6. You may add or remove the word to the sequence of the words by pressing Ctrl and clicking on the word with left mouse button.

Expected Results:
1. Markov chain is build for all the words in the text
1.2. This means that for the given chain of words (selected by used with Ctrl + Left Moust Click) the program shows:
1.2.1. What lexems follow this chain in the text (all cases) and thier number.
1.2.1. What lexems precede this chain in the text (all cases) and thier number.
2. During the file loading, the table of the longest repeated sequences of words (and thier number) is put in the log file in the "data" folder.
2.1. It shows all the repeated sequences where the number of words is equal to the longest repeated sequence and only them (not all repeated sequences in the text).
2.2. See log.txt file for the examples.

Performance requirements and resources allocation:
1. Start time on iCore i7-6700K + SSD is about 275 Mb of text file per minute.
1.1 Loading time depends linearly (!) from the file size
1.2. Onscreen forming of the word sequences is instantaneous. No delays.
1.3. No limitations of the length of the sequence.
1.4. No any kind of data caching by the programm is allowed, everything should be built from a scratch.
1.5. Performance is achieved with single CPU Core usage.
1.6. More cores usage is appreciated, but with a single core it must show the values above.
1.7. Application closure is instantaneous. No delays. No background closure.
2. Memory usage is about 267 Mb or RAM per 100 Mb of source data.
2.1. No external files used to store temporary data
2.2. Memory usage depends linearly (!) from the file size
