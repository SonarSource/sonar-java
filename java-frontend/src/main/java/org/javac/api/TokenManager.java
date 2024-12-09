package org.javac.api;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import java.util.Collections;
import javax.lang.model.element.Element;
import org.sonar.java.model.InternalSyntaxToken;

import static javax.tools.Diagnostic.NOPOS;
import static org.javac.api.JavacUtils.isImplicitConstructor;

public class TokenManager {

  private final CharSequence source;
  private final SourcePositions sourcePositions;
  private final Trees trees;
  private final CompilationUnitTree compilationUnit;
  private final LineMap lineMap;

  public TokenManager(CompilationUnitTree compilationUnitTree, Trees trees, CharSequence source) {
    this.source = source;
    this.sourcePositions = trees.getSourcePositions();
    this.compilationUnit = compilationUnitTree;
    this.lineMap = compilationUnitTree.getLineMap();
    this.trees = trees;
  }

  public InternalSyntaxToken getToken(int startPosition, int endPosition) {
    int startLine = (int) lineMap.getLineNumber(startPosition);
    int endLine = (int) lineMap.getLineNumber(endPosition);
    return new InternalSyntaxToken(
      startLine,
      (int) (startPosition - lineMap.getStartPosition(startLine)),
      source.subSequence(startPosition, endPosition).toString(),
      Collections.emptyList(),
      startLine != endLine);
  }

  public InternalSyntaxToken getEOFToken() {
    long endTokenIndex = sourcePositions.getEndPosition(compilationUnit, compilationUnit) - 1;
    while (source.charAt((int) endTokenIndex) == '\u0000') {
      endTokenIndex--;
    }
    return new InternalSyntaxToken(
      (int) lineMap.getLineNumber(endTokenIndex),
      (int) endTokenIndex,
      "",
      Collections.emptyList(),
      true);
  }

  public InternalSyntaxToken getOpenBraceToken(ClassTree classTree) {
    Tree firstMember = getFirstClassMember(classTree);
    long firstClassMemberStartPos = getNodeStartPosition(firstMember);
    int startPos = findFirst('{', (int) firstClassMemberStartPos, false);
    return getToken(startPos, startPos + 1);
  }

  public InternalSyntaxToken getCloseBraceToken(ClassTree classTree) {
    Tree lastMember = classTree.getMembers().get(classTree.getMembers().size() - 1);
    long lastClassMemberEndPos = getNodeEndPosition(lastMember);
    int endPos = findFirst('}', (int) lastClassMemberEndPos, true);
    return getToken(endPos, endPos + 1);
  }

  public InternalSyntaxToken getOpenParenToken(MethodTree methodTree) {
    long startPosition = getNodeStartPosition(methodTree.getBody());
    int startPos = findFirst('(', (int) startPosition, false);
    return getToken(startPos, startPos + 1);
  }

  public InternalSyntaxToken getCloseParenToken(MethodTree methodTree) {
    long startPosition = getNodeStartPosition(methodTree.getBody());
    int startPos = findFirst(')', (int) startPosition, false);
    return getToken(startPos, startPos + 1);
  }

  private int findFirst(char c, int startPos, boolean forward) {
    while (startPos >= 0 && startPos < source.length()) {
      if (source.charAt(startPos) == c) {
        break;
      }
      startPos += forward ? 1 : -1;
    }
    return startPos;
  }

  private int findChar(char c, int startPos) {
    for (int i = startPos; i < source.length(); i++) {
      if (source.charAt(i) == c) {
        return i;
      }
    }
    return -1;
  }

  public long getNodeStartLine(Tree node) {
    long startPosition = getNodeStartPosition(node);
    return startPosition == NOPOS ? NOPOS : lineMap.getLineNumber(startPosition);
  }

  public long getNodeEndLine(Tree node) {
    long endPosition = getNodeEndPosition(node);
    return endPosition == NOPOS ? NOPOS : lineMap.getLineNumber(endPosition);
  }

  public long getNodeStartColumn(Tree node) {
    long startPosition = getNodeStartPosition(node);
    return startPosition == NOPOS ? NOPOS : lineMap.getColumnNumber(startPosition);
  }

  public long getElementStartPosition(Element element) {
    TreePath path = trees.getPath(element);
    return getPathStartPosition(path);
  }

  public long getPathStartPosition(TreePath path) {
    while (path != null) {
      long startPosition = sourcePositions.getStartPosition(compilationUnit, path.getLeaf());
      if (startPosition != NOPOS) {
        return startPosition;
      }
      path = path.getParentPath();
    }
    return NOPOS;
  }

  public long getNodeStartPosition(Tree node) {
    TreePath path = getNodePath(node);
    return getPathStartPosition(path);
  }

  public long getNodeEndPosition(Tree node) {
    TreePath currentNode = getNodePath(node);
    while (node != null) {
      long endPosition = sourcePositions.getEndPosition(compilationUnit, currentNode.getLeaf());
      if (endPosition != NOPOS) {
        return endPosition;
      }
      currentNode = currentNode.getParentPath();
    }
    return NOPOS;
  }

  public TreePath getNodePath(Tree node) {
    return trees.getPath(compilationUnit, node);
  }

  private Tree getFirstClassMember(ClassTree classTree) {
    for (Tree member : classTree.getMembers()) {
      if (!isImplicitConstructor(member)) {
        return member;
      }
    }
    return null;
  }

}
