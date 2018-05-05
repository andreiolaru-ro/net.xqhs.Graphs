package net.xqhs.graphs.nlp;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TreeNode<T> {

	private T data;
	private List<TreeNode<T>> children = new ArrayList<>();
	private TreeNode parent;

	public TreeNode(T data) {
		this.data = data;
	}

	public void addChild(TreeNode child) {
		child.setParent(this);
		this.children.add(child);
	}

	public void addChild(T data) {
		TreeNode<T> newChild = new TreeNode<>(data);
		newChild.setParent(this);
		children.add(newChild);
	}

	public void addChildren(List<TreeNode<T>> children) {
		for (TreeNode t : children) {
			t.setParent(this);
		}
		this.children.addAll(children);
	}

	public List<TreeNode<T>> getChildren() {
		return children;
	}

	public List<T> getChildrenData() {
		System.out.println();
		System.out.println();
		System.out.println("All children of node:"
				+ this.data
				+ " : "
				+ getAllChildren().stream().map(x -> x.getData().toString())
						.collect(Collectors.joining(" ")));
		System.out.println();
		return getAllChildren().stream().map(x -> x.data)
				.collect(Collectors.toList());
	}

	public TreeNode addChildDuplicateSafe(TreeNode root, TreeNode child) {
		if (root.getChildrenData().contains(child.data)) {
			System.out.println(" Node " + child.data + " already in tree");
			return null;
		}
		child.setParent(this);
		this.children.add(child);
		return this;
	}

	public void setData(T data) {
		this.data = data;
	}

	public T getData() {
		return data;
	}

	public boolean containsSubtree(TreeNode<T> other) {
		if (this.getChildrenData().containsAll(other.getChildrenData())) {
			return true;
		}
		return false;
	}

	private void setParent(TreeNode parent) {
		this.parent = parent;
	}

	public TreeNode getParent() {
		return parent;
	}

	public ArrayList<TreeNode<T>> getAllChildren() {
		ArrayList<TreeNode<T>> result = new ArrayList<TreeNode<T>>();
		result.addAll(children);
		getChildren().stream().map(TreeNode::getAllChildren)
				.forEach(x -> result.addAll(x));

		return result;
	}

}
