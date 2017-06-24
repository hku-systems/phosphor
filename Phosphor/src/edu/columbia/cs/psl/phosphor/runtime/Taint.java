package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.LinkedList;

import java.io.Serializable;
import java.util.*;

// new design of Taint
// a taint is composed of a label(null or content, if null, then it has dependency)
// a dependency is just a list of dependent taint

public class Taint<T> implements Serializable {

	static class Pair<T> {
		T _1, _2;
		Pair(T a, T b) {
			_1 = a;
			_2 = b;
		}
	}

	private static final long serialVersionUID = 0x1111;

	public static boolean IGNORE_TAINTING;

	public static final <T> Taint<T> copyTaint(Taint<T> in)
	{
		return in;
//		if(in == null)
//			return null;
//		Taint<T> ret = new Taint<T>();
//		ret.copyFrom(in);
//		return ret;
	}

	protected void copyFrom(Taint<T> in) {
		lbl = in.lbl;
		dependencies = in.dependencies;
	}
	public Taint<T> copy()
	{
		return this;
//		if(IGNORE_TAINTING)
//			return this;
//		Taint<T> ret = new Taint<T>();
//		ret.lbl = lbl;
//		ret.dependencies = dependencies;
//		return ret;
	}
	//	public Object clone()  {
//		try {
//			Object ret = super.clone();
//			Taint r = (Taint) ret;
//			r.dependencies = (LinkedList<Taint>) dependencies.clone();
//			return ret;
//		} catch (CloneNotSupportedException e) {
//			e.printStackTrace();
//
//			return null;
//		}
//	}
//	@Override
	public String toString() {
		return "Taint lbl=" + lbl;
//		String depStr=" deps = [";
//		if(dependencies != null)
//		{
//			Node<T> dep = dependencies.getFirst();
//			while(dep != null)
//			{
//				if(dep.entry != null)
//				depStr += dep.entry + " ";
//				dep = dep.next;
//			}
//		}
//		depStr += "]";
//		return "Taint [lbl=" + lbl + " "+depStr+"]";
	}
	//	public Object debug;
	public Object lbl;
//	public LinkedList<T> dependencies;

	transient public LinkedList<EnqueuedTaint> enqueuedInControlFlow;

	transient public Pair<Taint> dependencies = null;

	private void getHelper(HashSet<Object> return_set, Taint t) {
		if (t.lbl != null) {
			if (Object[].class.isInstance(t.lbl))
				for (Object obj:
						(Object[])t.lbl) {
					return_set.add(obj);
				}
			else
				return_set.add(t.lbl);
			return;
		}
		Pair<Taint> pair = t.dependencies;
		if (pair == null)
			return;
		if (pair._1 != null) {
			getHelper(return_set ,pair._1);
		}
		if (pair._2 != null) {
			getHelper(return_set ,pair._2);
		}
	}

	public Object[] getOriginalTag() {
		HashSet<Object> return_set = new HashSet<Object>();
//		getHelper(return_set, this);
		java.util.LinkedList<Taint> toProcess = new java.util.LinkedList<>();
		toProcess.add(this);
		Taint top;
		Pair pair;
		while (!toProcess.isEmpty()) {
			top = toProcess.pop();
			if (top.lbl != null) {
				if (Object[].class.isInstance(top.lbl)) {
					for (Object obj:
							(Object[]) top.lbl)
						return_set.add(obj);
				} else {
					return_set.add(top.lbl);
				}
			} else {
				pair = top.dependencies;
				if (pair._1 != null)
					toProcess.add((Taint)pair._1);
				if (pair._2 != null)
					toProcess.add((Taint)pair._2);
			}
		}

		Object[] taints = return_set.toArray();
		if (taints.length > 0) {
			lbl = taints;
		}
		else
			lbl = null;
		dependencies = null;
		return taints;
	}

	// put the original tag to lbl
	public void preSerialization() {
		if (!Object[].class.isInstance(lbl)) {
			getOriginalTag();
		}
	}

	public Taint(T lbl) {
		this.lbl = lbl;
	}
	public Object getLabel() {
		return lbl;
	}
	public Pair getDependencies() {
		return dependencies;
	}
	public Taint(Taint<T> t1)
	{
		dependencies = t1.dependencies;
		lbl = t1.lbl;
//		if(Configuration.derivedTaintListener != null)
//			Configuration.derivedTaintListener.singleDepCreated(t1, this);
	}
	public Taint(Taint<T> t1, Taint<T> t2)
	{
//		if (true)
//			throw new IllegalArgumentException("error " + t1.toString() + " " + t2.toString());
		dependencies = new Pair(t1, t2);
//		if(Configuration.derivedTaintListener != null)
//			Configuration.derivedTaintListener.doubleDepCreated(t1, t2, this);
	}
	public Taint() {

	}
	public boolean addDependency(Taint<T> d)
	{
		try {
			throw new IllegalAccessException();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return true;
//		if(d == null)
//			return false;
//		boolean added = false;
//		if(d.lbl != null)
//			added = dependencies.addUnique(d.lbl);
//		added |= dependencies.addAll(d.dependencies);
//		return added;
	}
	public TaintedBooleanWithObjTag hasNoDependencies$$PHOSPHORTAGGED(ControlTaintTagStack ctrl, TaintedBooleanWithObjTag ret)
	{
		ret.val = hasNoDependencies();
		ret.taint = null;
		return ret;
	}
	public TaintedBooleanWithObjTag hasNoDependencies$$PHOSPHORTAGGED(TaintedBooleanWithObjTag ret)
	{
		ret.val = hasNoDependencies();
		ret.taint = null;
		return ret;
	}
	public boolean hasNoDependencies() {
		return dependencies == null;
	}
	public static <T> void combineTagsInPlace(Object obj, Taint<T> t1)
	{
		if(obj == null || t1 == null || IGNORE_TAINTING)
			return;
		@SuppressWarnings("unchecked")
		Taint<T> t = (Taint<T>) TaintUtils.getTaintObj(obj);
		if(t == null)
		{
			MultiTainter.taintedObject(obj, t1);
		}
		else
			t.addDependency(t1);
	}

//	@Override
//	public boolean equals(Object obj) {
//		if (obj instanceof Taint) {
//			return (lbl == null && dependencies == ((Taint)obj).dependencies) || (lbl == ((Taint)obj).lbl);
//		}
//		return false;
//	}

	public static <T> Taint<T> combineTags(Taint<T> t1, Taint<T> t2)
	{
		if(t1 == t2)
			return t1;
		if(t2 == null)
			return t1;
		if(t1 == null)
			return t2;
//		if(t1.lbl == null && t1.hasNoDependencies())
//			return t2;
//		if(t2.lbl == null && t2.hasNoDependencies())
//			return t1;
//		if(IGNORE_TAINTING)
//			return t1;
		Taint<T> r = new Taint<T>(t1, t2);
//		if(Configuration.derivedTaintListener != null)
//			Configuration.derivedTaintListener.doubleDepCreated(t1, t2, r);
		return r;
	}
	@SuppressWarnings("unchecked")
	public static <T> Taint<T> combineTags(Taint<T> t1, ControlTaintTagStack tags){
		if(t1 == null && tags.isEmpty())
			return null;
		else if(t1 == null)
		{
//			if(tags.isEmpty())
//				return null;
			return tags.taint;
		}
		else if(tags.isEmpty())
		{
//			if(t1.lbl == null && t1.hasNoDependencies())
//				return null;
			return t1;
		}
		else if(t1 == tags.taint)
			return t1;
		if(IGNORE_TAINTING)
			return t1;
		return new Taint<T>((Taint<T>) t1, tags.taint);
	}
	@SuppressWarnings("rawtypes")
	public static void combineTagsOnObject(Object o, ControlTaintTagStack tags)
	{
		if(tags.isEmpty() || IGNORE_TAINTING)
			return;
		if(o instanceof TaintedWithObjTag)
		{
			if(o instanceof String)
			{
				Taint onObj  = (Taint) ((TaintedWithObjTag)o).getPHOSPHOR_TAG();
				((String) o).PHOSPHOR_TAG = Taint.combineTags(onObj, tags);
//				for(int i = 0; i < ((String) o).length(); i++)
//				{
//					((String)o).valuePHOSPHOR_TAG[i] = Taint.combineTags(((String)o).valuePHOSPHOR_TAG[i], tags);
//				}
			}
			else
				((TaintedWithObjTag) o).setPHOSPHOR_TAG(Taint.combineTags((Taint) ((TaintedWithObjTag)o).getPHOSPHOR_TAG(), tags));

		}
	}

}