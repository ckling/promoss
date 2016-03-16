***************************
Hierarchical Multi-Dirichlet Process Topic Model (HMDP topic model)
***************************

(C) Copyright 2016, Christoph Carl Kling

Using functions from "Knoceans" by Gregor Heinrich Gregor Heinrich (gregor :: arbylon : net)
published under GNU GPL.

Tartarus Snowball stemmer by Martin Porter and Richard Boulton published under 
BSD License (see http://www.opensource.org/licenses/bsd-license.html ), with Copyright 
(c) 2001, Dr Martin Porter, and (for the Java developments) Copyright (c) 2002, 
Richard Boulton. 

Java Delaunay Triangulation (JDT) by boaz88 :: gmail : com published under Apache License 2.0 
(http://www.apache.org/licenses/LICENSE-2.0)

PCFSTM is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by the Free 
Software Foundation; either version 3 of the License, or (at your option) 
any later version.

PCFSTM is distributed in the hope that it will be useful, but WITHOUT 
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program; if not, write to the Free Software Foundation, Inc., 59 Temple
Place, Suite 330, Boston, MA 02111-1307 USA

***************************
Notes
***************************

This is the practical collapsed stochastic variational Bayesian inference (PCSVB) for the HMDP.

A detailed tutorial and documentation of the HMDP is in preparation.

In future versions, there will be:
- support for typical metadata, i.e. temporal and geographical context
- a distributed version of PCSVB of HMDP based on Hadoop 

The goal is to create a easy-to-use, scalable topic model for arbitrary context.
