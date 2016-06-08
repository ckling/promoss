***************************
Input file format
***************************
--------------------------- 
texts.txt
--------------------------- 
Each line corresponds to a document. First, the context group IDs (for each context one) are given, separated by commas. The context group in context 0 is given first, then the context group in context 1 and so on. Then follows a space and the words of the documents separated by spaces. 
Example file:
254,531,790,157,0  exist distribut origin softwar distributor agre gpl
254,528,789,157,0  gpl establish term distribut origin softwar even goe unmodifi word distribut gpl softwar one agre 
254,901,700,157,0  dynam link constitut make deriv work allow dynam link long rule follow code make deriv work rule
254,838,691,157,0  gpl also deal deriv work link creat deriv work gpl affect gpl defin scope copyright law gpl section 

--------------------------- 
groups.txt
--------------------------- 
Each line gives the parent context clusters of a context group. Data are separated by spaces. The first column gives the context id, the second column gives the group ID of the context group, and then the IDs of the context clusters from which the documents of that context group draw their topics are given.
Example file:
0 0 0 1
0 1 0 1 2
0 2 1 2 3
0 3 2 3 4
0 4 3 4 5
0 5 4 5 6
0 6 5 6 7
0 7 6 7 8
0 8 7 8 9
0 9 8 9 10
0 10 9 10 11

The first line reads: For context 0, documents which are assigned to context group 0 draw their topics from context cluster 0 and context cluster 1.
If no groups.txt is given, all context groups will be linked to a context cluster with the same ID, which means that all context clusters are independent.
