/*
 * MIT License
 *
 * Copyright (c) 2024 Nima Karimipour
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.ucr.cs.riple.injector.util;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.injector.exceptions.TargetClassNotFound;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Utility class for working with nodes in AST in Java and Javaparser. */
public class ASTUtils {

  /**
   * Extracts the callable simple name from callable signature. (e.g. on input "run(Object i)"
   * returns "run").
   *
   * @param signature callable signature in string.
   * @return callable simple name.
   */
  public static String extractCallableName(String signature) {
    StringBuilder ans = new StringBuilder();
    int level = 0;
    for (int i = 0; i < signature.length(); i++) {
      char current = signature.charAt(i);
      if (current == '(') {
        break;
      }
      switch (current) {
        case '>':
          ++level;
          break;
        case '<':
          --level;
          break;
        default:
          if (level == 0) {
            ans.append(current);
          }
      }
    }
    return ans.toString();
  }

  /**
   * Checks if node is a type declaration or an anonymous class.
   *
   * @param node Node instance.
   * @return true if is a type declaration or an anonymous class and false otherwise.
   */
  public static boolean isTypeDeclarationOrAnonymousClass(Node node) {
    return node instanceof ClassOrInterfaceDeclaration
        || node instanceof EnumDeclaration
        || node instanceof AnnotationDeclaration
        || (node instanceof ObjectCreationExpr
            && ((ObjectCreationExpr) node).getAnonymousClassBody().isPresent());
  }

  /**
   * Returns {@link NodeList} containing all members of an
   * Enum/Interface/Class/AnonymousClass/Annotation Declaration by flat name from a compilation unit
   * tree.
   *
   * @param cu Compilation Unit tree instance.
   * @param flatName Flat name in string.
   * @return {@link NodeList} containing all members
   * @throws TargetClassNotFound if the target class is not found.
   */
  public static NodeList<BodyDeclaration<?>> getTypeDeclarationMembersByFlatName(
      CompilationUnit cu, String flatName) throws TargetClassNotFound {
    String packageName;
    Optional<PackageDeclaration> packageDeclaration = cu.getPackageDeclaration();
    if (packageDeclaration.isPresent()) {
      packageName = packageDeclaration.get().getNameAsString();
    } else {
      packageName = "";
    }
    Preconditions.checkArgument(
        flatName.startsWith(packageName),
        "Package name of compilation unit is incompatible with class name: "
            + packageName
            + " : "
            + flatName);
    String flatNameExcludingPackageName =
        packageName.isEmpty() ? flatName : flatName.substring(packageName.length() + 1);
    List<String> keys = new ArrayList<>(Arrays.asList(flatNameExcludingPackageName.split("\\$")));
    Node cursor = findTopLevelClassDeclarationOnCompilationUnit(cu, keys.get(0));
    keys.remove(0);
    for (String key : keys) {
      String indexString = extractIntegerFromBeginningOfStringInString(key);
      String actualName = key.substring(indexString.length());
      int index = indexString.isEmpty() ? 0 : Integer.parseInt(indexString) - 1;
      Preconditions.checkNotNull(cursor);
      if (key.matches("\\d+")) {
        cursor = findAnonymousClassOrEnumConstant(cursor, index);
      } else {
        cursor =
            indexString.isEmpty()
                ? findDirectInnerClass(cursor, actualName)
                : findNonDirectInnerClass(cursor, actualName, index);
      }
    }
    return getMembersOfNode(cursor);
  }

  /**
   * Locates a variable declaration expression in the tree of a {@link CallableDeclaration} with the
   * given name. Please note that the target local variable must be declared inside a callable
   * declaration and local variables inside initializer blocks or lambdas are not supported.
   *
   * @param encMethod The enclosing method which the variable is declared in.
   * @param varName The name of the variable.
   * @return The variable declaration expression, or null if it is not found.
   */
  @Nullable
  public static VariableDeclarationExpr locateVariableDeclarationExpr(
      CallableDeclaration<?> encMethod, String varName) {
    // Should not visit inner nodes of inner methods in the given method, since the given
    // method should be the closest enclosing method of the target local variable. Therefore, we
    // use DirectMethodParentIterator to skip inner methods.
    Iterator<Node> treeIterator = new ASTUtils.DirectMethodParentIterator(encMethod);
    while (treeIterator.hasNext()) {
      Node n = treeIterator.next();
      if (n instanceof VariableDeclarationExpr) {
        VariableDeclarationExpr v = (VariableDeclarationExpr) n;
        if (v.getVariables().stream()
            .anyMatch(variableDeclarator -> variableDeclarator.getNameAsString().equals(varName))) {
          return v;
        }
      }
    }
    return null;
  }

  /**
   * Extracts simple name of fully qualified name. (e.g. for "{@code a.c.b.Foo<a.b.Bar, a.c.b.Foo>}"
   * will return "{@code Foo<Bar,Foo>}").
   *
   * @param name Fully qualified name.
   * @return simple name.
   */
  public static String simpleName(String name) {
    int index = 0;
    StringBuilder ans = new StringBuilder();
    StringBuilder tmp = new StringBuilder();
    while (index < name.length()) {
      char c = name.charAt(index);
      switch (c) {
        case ' ':
        case '<':
        case '>':
        case ',':
          ans.append(tmp);
          ans.append(c);
          tmp = new StringBuilder();
          break;
        case '.':
          tmp = new StringBuilder();
          break;
        default:
          tmp.append(c);
      }
      index++;
    }
    if (!name.isEmpty()) {
      ans.append(tmp);
    }
    return ans.toString().replaceAll(" ", "");
  }

  /**
   * Extracts the package name from fully qualified name. (e.g. for "{@code a.c.b.Foo<a.b.Bar,
   * a.c.b.Foo>}" will return "{@code a.c.b}").
   *
   * @param name Fully qualified name in String.
   * @return Package name.
   */
  public static String getPackageName(String name) {
    if (!name.contains(".")) {
      return null;
    }
    List<String> verified = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    int index = 0;
    while (index < name.length()) {
      char currentChar = name.charAt(index);
      if (currentChar == '.') {
        verified.add(current.toString());
        current = new StringBuilder();
      } else {
        if (Character.isAlphabetic(currentChar) || Character.isDigit(currentChar)) {
          current.append(currentChar);
        } else {
          break;
        }
      }
      index++;
    }
    return String.join(".", verified);
  }

  /**
   * Returns containing static initializer blocks of a {@link BodyDeclaration}.
   *
   * @param bodyDeclaration the body declaration to get its static initializer blocks.
   * @return the static initializer blocks of the body declaration.
   */
  public static Set<InitializerDeclaration> getStaticInitializerBlocks(
      BodyDeclaration<?> bodyDeclaration) {
    return bodyDeclaration.getChildNodes().stream()
        .filter(
            node ->
                node instanceof InitializerDeclaration
                    && ((InitializerDeclaration) node).isStatic())
        .map(node -> (InitializerDeclaration) node)
        .collect(Collectors.toSet());
  }

  /**
   * Finds Top-Level type declaration within a {@link CompilationUnit} tree by name, this node is a
   * direct child of the compilation unit tree and can be: [{@link ClassOrInterfaceDeclaration},
   * {@link EnumDeclaration}, {@link AnnotationDeclaration}].
   *
   * @param tree instance of compilation unit tree.
   * @param name name of the declaration.
   * @return the typeDeclaration with the given name.
   * @throws TargetClassNotFound if the target class is not found.
   */
  @Nonnull
  private static TypeDeclaration<?> findTopLevelClassDeclarationOnCompilationUnit(
      CompilationUnit tree, String name) throws TargetClassNotFound {
    Optional<ClassOrInterfaceDeclaration> classDeclaration = tree.getClassByName(name);
    if (classDeclaration.isPresent()) {
      return classDeclaration.get();
    }
    Optional<EnumDeclaration> enumDeclaration = tree.getEnumByName(name);
    if (enumDeclaration.isPresent()) {
      return enumDeclaration.get();
    }
    Optional<AnnotationDeclaration> annotationDeclaration =
        tree.getAnnotationDeclarationByName(name);
    if (annotationDeclaration.isPresent()) {
      return annotationDeclaration.get();
    }
    Optional<ClassOrInterfaceDeclaration> interfaceDeclaration = tree.getInterfaceByName(name);
    if (interfaceDeclaration.isPresent()) {
      return interfaceDeclaration.get();
    }
    Optional<RecordDeclaration> recordDeclaration = tree.getRecordByName(name);
    if (recordDeclaration.isPresent()) {
      return recordDeclaration.get();
    }
    throw new TargetClassNotFound("Top-Level", name, tree);
  }

  /**
   * Locates the inner class with the given name which is directly connected to cursor.
   *
   * @param cursor Parent node of inner class.
   * @param name Name of the inner class.
   * @return inner class with the given name.
   * @throws TargetClassNotFound if the target class is not found.
   */
  private static Node findDirectInnerClass(Node cursor, String name) throws TargetClassNotFound {
    List<Node> nodes = new ArrayList<>();
    cursor.walk(
        Node.TreeTraversal.DIRECT_CHILDREN,
        node -> {
          if (isDeclarationWithName(node, name)) {
            nodes.add(node);
          }
        });
    if (nodes.isEmpty()) {
      throw new TargetClassNotFound("Direct-Inner-Class", name, cursor);
    }
    return nodes.get(0);
  }

  /**
   * Locates the non-direct inner class with the given name at specific index.
   *
   * @param cursor Starting node for traversal.
   * @param name name of the inner class.
   * @param index index of the desired node among the candidates.
   * @return inner class with the given name and index.
   * @throws TargetClassNotFound if the target class is not found.
   */
  private static Node findNonDirectInnerClass(Node cursor, String name, int index)
      throws TargetClassNotFound {
    final List<Node> candidates = new ArrayList<>();
    walk(
        cursor,
        candidates,
        node ->
            isDeclarationWithName(node, name)
                && node.getParentNode().isPresent()
                && !node.getParentNode().get().equals(cursor));
    if (index >= candidates.size()) {
      throw new TargetClassNotFound("Non-Direct-Inner-Class", index + name, cursor);
    }
    return candidates.get(index);
  }

  /**
   * Checks if the node is of type {@link TypeDeclaration} and a specific name.
   *
   * @param node input node.
   * @param name name.
   * @return true the node is subtype of TypeDeclaration and has the given name.
   */
  private static boolean isDeclarationWithName(Node node, String name) {
    if (node instanceof TypeDeclaration<?>) {
      return ((TypeDeclaration<?>) node).getNameAsString().equals(name);
    }
    return false;
  }

  /**
   * Locates an anonymous class or enum constant at specific index.
   *
   * @param cursor Starting node for traversal.
   * @param index index.
   * @return anonymous class or enum constant at specific index.
   * @throws TargetClassNotFound if the target class is not found.
   */
  private static Node findAnonymousClassOrEnumConstant(Node cursor, int index)
      throws TargetClassNotFound {
    final List<Node> candidates = new ArrayList<>();
    if (cursor instanceof EnumDeclaration) {
      // According to the Java language specification, enum constants are the first members of
      // enums. In JavaParser's data structures, enum constants are stored after all other members
      // of the enum which does not conform to how javac assigns flat names. We prioritize visiting
      // enum constants first.
      NodeList<EnumConstantDeclaration> constants = ((EnumDeclaration) cursor).getEntries();
      if (index < constants.size()) {
        return constants.get(index);
      }
      index -= constants.size();
    }
    walk(
        cursor,
        candidates,
        node -> {
          if (node instanceof ObjectCreationExpr) {
            return ((ObjectCreationExpr) node).getAnonymousClassBody().isPresent();
          }
          return false;
        });
    if (index >= candidates.size()) {
      throw new TargetClassNotFound("Top-Level-Anonymous-Class", "$" + index, cursor);
    }
    return candidates.get(index);
  }

  /**
   * Returns members of the type declaration.
   *
   * @param node Node instance.
   * @return {@link NodeList} containing member of node.
   */
  private static NodeList<BodyDeclaration<?>> getMembersOfNode(Node node) {
    if (node == null) {
      return null;
    }
    if (node instanceof EnumDeclaration) {
      return ((EnumDeclaration) node).getMembers();
    }
    if (node instanceof ClassOrInterfaceDeclaration) {
      return ((ClassOrInterfaceDeclaration) node).getMembers();
    }
    if (node instanceof AnnotationDeclaration) {
      return ((AnnotationDeclaration) node).getMembers();
    }
    if (node instanceof ObjectCreationExpr) {
      return ((ObjectCreationExpr) node).getAnonymousClassBody().orElse(null);
    }
    if (node instanceof EnumConstantDeclaration) {
      return ((EnumConstantDeclaration) node).getClassBody();
    }
    if (node instanceof RecordDeclaration) {
      return ((RecordDeclaration) node).getMembers();
    }
    return null;
  }

  /**
   * Extracts the integer at the start of string (e.g. 129uid -> 129).
   *
   * @param key string containing the integer.
   * @return the integer at the start of the key, empty if no digit found at the beginning (e.g.
   *     u129 -> empty)
   */
  private static String extractIntegerFromBeginningOfStringInString(String key) {
    int index = 0;
    while (index < key.length()) {
      char c = key.charAt(index);
      if (!Character.isDigit(c)) {
        break;
      }
      index++;
    }
    return key.substring(0, index);
  }

  /**
   * Walks on the AST starting from the cursor in {@link
   * com.github.javaparser.ast.Node.TreeTraversal#DIRECT_CHILDREN} manner, and adds all visiting
   * nodes which holds the predicate.
   *
   * @param cursor starting node for traversal.
   * @param candidates list of candidates, an empty list should be passed at the call site, accepted
   *     visited nodes will be added to this list.
   * @param predicate predicate to check if a node should be added to list of candidates.
   */
  private static void walk(Node cursor, List<Node> candidates, Predicate<Node> predicate) {
    cursor.walk(
        Node.TreeTraversal.DIRECT_CHILDREN,
        node -> {
          if (!isTypeDeclarationOrAnonymousClass(node)) {
            walk(node, candidates, predicate);
          }
          if (predicate.test(node)) {
            candidates.add(node);
          }
        });
  }

  /**
   * Iterates over children of a {@link CallableDeclaration} in a depth-first manner, skipping over
   * any {@link BodyDeclaration}.
   */
  private static final class DirectMethodParentIterator implements Iterator<Node> {

    private final ArrayDeque<Node> deque = new ArrayDeque<>();

    public DirectMethodParentIterator(CallableDeclaration<?> node) {
      deque.add(node);
    }

    @Override
    public boolean hasNext() {
      return !deque.isEmpty();
    }

    @Override
    public Node next() {
      Node next = deque.removeFirst();
      List<Node> children = next.getChildNodes();
      for (int i = children.size() - 1; i >= 0; i--) {
        Node child = children.get(i);
        if (!(child instanceof BodyDeclaration<?>)) {
          // Skip over any CallableDeclaration.
          deque.add(children.get(i));
        }
      }
      return next;
    }
  }
}
