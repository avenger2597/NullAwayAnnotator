/*
 * Copyright (c) 2023 University of California, Riverside.
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

package edu.ucr.cs.riple.injector.changes;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import com.github.javaparser.ast.type.Type;
import com.google.common.collect.ImmutableList;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.modifications.Modification;
import edu.ucr.cs.riple.injector.modifications.MultiPositionModification;
import edu.ucr.cs.riple.injector.util.TypeUtils;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

/** Represents a type-use annotation change on types in the AST. */
public abstract class TypeUseAnnotationChange extends AnnotationChange {

  /** List of indices that represent the position of the type argument in the node's type. */
  public ImmutableList<ImmutableList<Integer>> typeIndex;

  public TypeUseAnnotationChange(
      Location location, Name annotation, ImmutableList<ImmutableList<Integer>> typeIndex) {
    super(location, annotation);
    this.typeIndex = typeIndex;
  }

  /**
   * Computes the text modification on the given type argument. It does not modify the containing
   * type arguments of the given type.
   */
  @Nullable
  public abstract Modification computeTextModificationOnType(
      Type type, AnnotationExpr annotationExpr);

  /**
   * Computes the text modification on the given node if required. It does not modify the containing
   * type arguments.
   */
  public abstract <T extends NodeWithAnnotations<?> & NodeWithRange<?>>
      Modification computeTextModificationOnNode(T node, AnnotationExpr annotationExpr);

  @Nullable
  @Override
  public <T extends NodeWithAnnotations<?> & NodeWithRange<?>>
      Modification computeTextModificationOn(T node) {
    Set<Modification> modifications = new HashSet<>();
    AnnotationExpr annotationExpr = new MarkerAnnotationExpr(annotationName.simpleName);
    Type type = TypeUtils.getTypeFromNode(node);
    Modification onNode = computeTextModificationOnNode(node, annotationExpr);
    if (onNode != null) {
      modifications.add(onNode);
    }
    // Check if the expression is a variable declaration with an initializer.
    Type initializedType = getInitializedType(node);
    for (ImmutableList<Integer> index : typeIndex) {
      if (index.size() == 1 && index.get(0) == 0) {
        // Already added on declaration.
        continue;
      }
      // Apply the change on type arguments.
      modifications.addAll(type.accept(new TypeArgumentChangeVisitor(index, annotationExpr), this));
      if (initializedType != null) {
        modifications.addAll(
            initializedType.accept(new TypeArgumentChangeVisitor(index, annotationExpr), this));
      }
    }
    return modifications.isEmpty() ? null : new MultiPositionModification(modifications);
  }

  /**
   * Retrieves the type of the object being initialized within the given node, if applicable. This
   * method handles nodes representing variable declarations or field declarations that may contain
   * an initializer expression. If the initializer is an object creation expression, the type of the
   * created object is returned.
   *
   * @param node the node from which to extract the initialized type, either a {@code
   *     VariableDeclarationExpr} or a {@code FieldDeclaration}.
   * @param <T> a node that extends both {@code NodeWithAnnotations} and {@code NodeWithRange}.
   * @return the {@code Type} of the object being initialized if the initializer is an {@code
   *     ObjectCreationExpr}, or {@code null} if no such type can be determined.
   */
  private static <T extends NodeWithAnnotations<?> & NodeWithRange<?>> Type getInitializedType(
      T node) {
    Type initializedType = null;
    if (node instanceof VariableDeclarationExpr) {
      VariableDeclarationExpr vde = (VariableDeclarationExpr) node;
      if (!vde.getVariables().isEmpty()) {
        if (vde.getVariables().get(0).getInitializer().isPresent()) {
          Expression initializedValue = vde.getVariables().get(0).getInitializer().get();
          if (initializedValue instanceof ObjectCreationExpr) {
            initializedType = ((ObjectCreationExpr) initializedValue).getType();
          }
        }
      }
    }
    if (node instanceof FieldDeclaration) {
      FieldDeclaration fieldDeclaration = (FieldDeclaration) node;
      for (int i = 0; i < fieldDeclaration.getVariables().size(); i++) {
        if (fieldDeclaration.getVariables().get(i).getInitializer().isPresent()) {
          Expression initializedValue =
              fieldDeclaration.getVariables().get(i).getInitializer().get();
          if (initializedValue instanceof ObjectCreationExpr) {
            initializedType = ((ObjectCreationExpr) initializedValue).getType();
          }
        }
      }
    }
    return initializedType;
  }

  @Override
  public boolean equals(Object o) {
    boolean ans = super.equals(o);
    if (!(o instanceof TypeUseAnnotationChange)) {
      return false;
    }
    if (!ans) {
      return false;
    }
    return typeIndex.equals(((TypeUseAnnotationChange) o).typeIndex);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), typeIndex);
  }

  /**
   * Returns the list of indices that represent the position of the type argument in the node's
   * type.
   *
   * @return List of indices.
   */
  public ImmutableList<ImmutableList<Integer>> getTypeIndex() {
    return typeIndex;
  }
}
