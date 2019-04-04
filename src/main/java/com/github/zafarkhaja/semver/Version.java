/*
 * The MIT License
 *
 * Copyright 2012-2016 Zafar Khaja <zafarkhaja@gmail.com>.
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

package com.github.zafarkhaja.semver;

import com.github.zafarkhaja.semver.compiling.LexerException;
import com.github.zafarkhaja.semver.compiling.UnexpectedTokenException;
import com.github.zafarkhaja.semver.expr.ExpressionParser;
import com.github.zafarkhaja.semver.expr.MavenParser;
import java.io.Serializable;
import java.util.Comparator;
import java.util.function.Predicate;

/**
 * The {@code Version} class is the main class of the Java SemVer library.
 *
 * <p>This class implements the Facade design pattern.
 * It is also immutable, which makes the class thread-safe.
 *
 * @author Zafar Khaja &lt;zafarkhaja@gmail.com&gt;
 * @since 0.1.0
 */
public class Version implements Comparable<Version>, Serializable {

  /**
   * A comparator that respects the build metadata when comparing versions.
   */
  public static final Comparator<Version> BUILD_AWARE_ORDER = new BuildAwareOrder();
  private static final long serialVersionUID = -2008891377046871654L;
  /**
   * A separator that separates the pre-release
   * version from the normal version.
   */
  private static final String PRE_RELEASE_PREFIX = "-";
  /**
   * A separator that separates the build metadata from
   * the normal version or the pre-release version.
   */
  private static final String BUILD_PREFIX = "+";
  /**
   * The normal version.
   */
  private final NormalVersion normal;
  /**
   * The pre-release version.
   */
  private final MetadataVersion preRelease;
  /**
   * The build metadata.
   */
  private final MetadataVersion build;

  /**
   * Constructs a {@code Version} instance with the normal version.
   *
   * @param normal the normal version
   */
  Version(NormalVersion normal) {
    this(normal, MetadataVersion.NULL, MetadataVersion.NULL);
  }

  /**
   * Constructs a {@code Version} instance with the
   * normal version and the pre-release version.
   *
   * @param normal     the normal version
   * @param preRelease the pre-release version
   */
  Version(NormalVersion normal, MetadataVersion preRelease) {
    this(normal, preRelease, MetadataVersion.NULL);
  }

  /**
   * Constructs a {@code Version} instance with the normal
   * version, the pre-release version and the build metadata.
   *
   * @param normal     the normal version
   * @param preRelease the pre-release version
   * @param build      the build metadata
   */
  Version(NormalVersion normal, MetadataVersion preRelease, MetadataVersion build) {
    this.normal = normal;
    this.preRelease = preRelease;
    this.build = build;
  }

  /**
   * Creates a new instance of {@code Version} as a
   * result of parsing the specified version string.
   *
   * @param version the version string to parse
   * @return a new instance of the {@code Version} class
   * @throws IllegalArgumentException     if the input string is {@code NULL} or empty
   * @throws ParseException               when invalid version string is provided
   * @throws UnexpectedCharacterException is a special case of {@code ParseException}
   */
  public static Version valueOf(String version) {
    return VersionParser.parseValidSemVer(version);
  }

  /**
   * Creates a new instance of {@code Version}
   * for the specified version numbers.
   *
   * @param major the major version number
   * @return a new instance of the {@code Version} class
   * @throws IllegalArgumentException if a negative integer is passed
   * @since 0.7.0
   */
  public static Version forIntegers(int major) {
    return new Version(new NormalVersion(major, 0, 0));
  }

  /**
   * Creates a new instance of {@code Version}
   * for the specified version numbers.
   *
   * @param major the major version number
   * @param minor the minor version number
   * @return a new instance of the {@code Version} class
   * @throws IllegalArgumentException if a negative integer is passed
   * @since 0.7.0
   */
  public static Version forIntegers(int major, int minor) {
    return new Version(new NormalVersion(major, minor, 0));
  }

  /**
   * Creates a new instance of {@code Version}
   * for the specified version numbers.
   *
   * @param major the major version number
   * @param minor the minor version number
   * @param patch the patch version number
   * @return a new instance of the {@code Version} class
   * @throws IllegalArgumentException if a negative integer is passed
   * @since 0.7.0
   */
  public static Version forIntegers(int major, int minor, int patch) {
    return new Version(new NormalVersion(major, minor, patch));
  }

  /**
   * Evaluates whether the given String compiles to a valid semver.
   * @param toCheck the string to check
   * @return true if it is, false otherwise
   */
  public static boolean isValid(String toCheck) {
    try {
      VersionParser.parseValidSemVer(toCheck);
      return true;
    } catch (ParseException e) {
      return false;
    }
  }

  /**
   * Checks if this version satisfies the specified Expression string.
   * First checks for JSemVer Expression, if that fails it checks for a Maven-style version range.
   *
   * <p>This method is a part of the Expressions API.
   *
   * @param expr the Expression string
   * @return {@code true} if this version satisfies the specified
   *     Expression or {@code false} otherwise
   * @throws ParseException           in case of a general parse error
   * @throws LexerException           when encounters an illegal character
   * @throws UnexpectedTokenException when comes across an unexpected token
   * @since 0.10.0
   */
  public boolean satisfies(String expr) {
    Predicate<Version> res;
    try {
      res = ExpressionParser.newInstance().parse(expr);
    } catch (ParseException e) {
      try {
        res = new MavenParser().parse(expr);
      } catch (ParseException e2) {
        res = version -> false;
      }
    }
    return satisfies(res);
  }

  /**
   * Checks if this version satisfies the specified Expression.
   *
   * <p>This method is a part of the Expressions API.
   *
   * @param expr the Expression
   * @return {@code true} if this version satisfies the specified
   *     Expression or {@code false} otherwise
   * @since 0.10.0
   */
  public boolean satisfies(Predicate<Version> expr) {
    return expr.test(this);
  }

  /**
   * Checks if this version satisfies the specified Maven version range string.
   *
   * <p>This method is a part of the Expressions API.
   *
   * @param expr the Maven version range string
   * @return {@code true} if this version satisfies the specified
   *     range or {@code false} otherwise
   * @throws ParseException           in case of a general parse error
   * @throws LexerException           when encounters an illegal character
   * @throws UnexpectedTokenException when comes across an unexpected token
   * @since 0.10.0
   */
  public boolean satisfiesJSemVerExpression(String expr) {
    return satisfies(ExpressionParser.newInstance().parse(expr));
  }

  /**
   * Checks if this version satisfies the specified SemVer Expression string.
   *
   * <p>This method is a part of the Expressions API.
   *
   * @param expr the JSemVer Expression string
   * @return {@code true} if this version satisfies the specified
   *     JSemVer Expression or {@code false} otherwise
   * @throws ParseException           in case of a general parse error
   * @throws LexerException           when encounters an illegal character
   * @throws UnexpectedTokenException when comes across an unexpected token
   * @since 0.10.0
   */
  public boolean satisfiesMavenRange(String expr) {
    return satisfies(new MavenParser().parse(expr));
  }

  /**
   * Re-sets the major version number.
   *
   * @param major        the new major version number
   * @param preserveMeta whether to preserve version metadata
   * @return a new {@code Version} with the specified major version number
   * @throws IllegalArgumentException if the number is negative
   */
  public Version withMajor(int major, boolean preserveMeta) {
    if (preserveMeta) {
      return new Version(normal.withMajor(major), preRelease, build);
    }
    return new Version(normal.withMajor(major));
  }

  /**
   * Re-sets the minor version number.
   *
   * @param minor        the new major version number
   * @param preserveMeta whether to preserve version metadata
   * @return a new {@code Version} with the specified major version number
   * @throws IllegalArgumentException if the number is negative
   */
  public Version withMinor(int minor, boolean preserveMeta) {
    if (preserveMeta) {
      return new Version(normal.withMinor(minor), preRelease, build);
    }
    return new Version(normal.withMinor(minor));
  }

  /**
   * Re-sets the patch version number.
   *
   * @param patch        the new major version number
   * @param preserveMeta whether to preserve version metadata
   * @return a new {@code Version} with the specified major version number
   * @throws IllegalArgumentException if the number is negative
   */
  public Version withPatch(int patch, boolean preserveMeta) {
    if (preserveMeta) {
      return new Version(normal.withPatch(patch), preRelease, build);
    }
    return new Version(normal.withPatch(patch));
  }

  /**
   * Increments the major version.
   *
   * @return a new instance of the {@code Version} class
   */
  public Version incrementMajorVersion() {
    if (preRelease == MetadataVersion.NULL || normal.getPatch() != 0 || normal.getMinor() != 0) {
      return new Version(normal.incrementMajor());
    }
    return new Version(normal);
  }

  /**
   * Increments the major version and appends the pre-release version.
   *
   * @param preRelease the pre-release version to append
   * @return a new instance of the {@code Version} class
   * @throws IllegalArgumentException     if the input string is {@code NULL} or empty
   * @throws ParseException               when invalid version string is provided
   * @throws UnexpectedCharacterException is a special case of {@code ParseException}
   */
  public Version incrementMajorVersion(String preRelease) {
    return new Version(
        normal.incrementMajor(),
        VersionParser.parsePreRelease(preRelease)
    );
  }

  public Version incrementPreMajorVersion() {
    return incrementMajorVersion("0");
  }

  /**
   * Increments the minor version.
   *
   * @return a new instance of the {@code Version} class
   */
  public Version incrementMinorVersion() {
    if (preRelease == MetadataVersion.NULL || normal.getPatch() != 0) {
      return new Version(normal.incrementMinor());
    }
    return new Version(normal);
  }

  /**
   * Increments the minor version and appends the pre-release version.
   *
   * @param preRelease the pre-release version to append
   * @return a new instance of the {@code Version} class
   * @throws IllegalArgumentException     if the input string is {@code NULL} or empty
   * @throws ParseException               when invalid version string is provided
   * @throws UnexpectedCharacterException is a special case of {@code ParseException}
   */
  public Version incrementMinorVersion(String preRelease) {
    return new Version(
        normal.incrementMinor(),
        VersionParser.parsePreRelease(preRelease)
    );
  }

  /**
   * Increments the minor version.
   *
   * @return a new instance of the {@code Version} class
   */
  public Version incrementPreMinorVersion() {
    return incrementMinorVersion("0");
  }

  /**
   * Increments the patch version.
   *
   * @return a new instance of the {@code Version} class
   */
  public Version incrementPatchVersion() {
    if (preRelease == MetadataVersion.NULL) {
      return new Version(normal.incrementPatch());
    }
    return new Version(normal);
  }

  /**
   * Increments the patch version and appends the pre-release version.
   *
   * @param preRelease the pre-release version to append
   * @return a new instance of the {@code Version} class
   * @throws IllegalArgumentException     if the input string is {@code NULL} or empty
   * @throws ParseException               when invalid version string is provided
   * @throws UnexpectedCharacterException is a special case of {@code ParseException}
   */
  public Version incrementPatchVersion(String preRelease) {
    return new Version(
        normal.incrementPatch(),
        VersionParser.parsePreRelease(preRelease)
    );
  }

  public Version incrementPrePatchVersion() {
    return incrementPatchVersion("0");
  }

  /**
   * Increments the pre-release version.
   *
   * @return a new instance of the {@code Version} class
   */
  public Version incrementPreReleaseVersion() {
    return new Version(normal, preRelease.increment());
  }

  /**
   * Increments the build metadata.
   *
   * @return a new instance of the {@code Version} class
   */
  public Version incrementBuildMetadata() {
    return new Version(normal, preRelease, build.increment());
  }

  /**
   * Returns the major version number.
   *
   * @return the major version number
   */
  public int getMajorVersion() {
    return normal.getMajor();
  }

  /**
   * Returns the minor version number.
   *
   * @return the minor version number
   */
  public int getMinorVersion() {
    return normal.getMinor();
  }

  /**
   * Returns the patch version number.
   *
   * @return the patch version number
   */
  public int getPatchVersion() {
    return normal.getPatch();
  }

  /**
   * Returns the string representation of the normal version.
   *
   * @return the string representation of the normal version
   */
  public String getNormalVersion() {
    return normal.toString();
  }

  /**
   * Returns the string representation of the pre-release version.
   *
   * @return the string representation of the pre-release version
   */
  public String getPreReleaseVersion() {
    return preRelease.toString();
  }

  /**
   * Sets the pre-release version.
   *
   * @param preRelease the pre-release version to set
   * @return a new instance of the {@code Version} class
   * @throws IllegalArgumentException     if the input string is {@code NULL} or empty
   * @throws ParseException               when invalid version string is provided
   * @throws UnexpectedCharacterException is a special case of {@code ParseException}
   */
  public Version setPreReleaseVersion(String preRelease) {
    return new Version(normal, VersionParser.parsePreRelease(preRelease));
  }

  /**
   * Returns the string representation of the build metadata.
   *
   * @return the string representation of the build metadata
   */
  public String getBuildMetadata() {
    return build.toString();
  }

  /**
   * Sets the build metadata.
   *
   * @param build the build metadata to set
   * @return a new instance of the {@code Version} class
   * @throws IllegalArgumentException     if the input string is {@code NULL} or empty
   * @throws ParseException               when invalid version string is provided
   * @throws UnexpectedCharacterException is a special case of {@code ParseException}
   */
  public Version setBuildMetadata(String build) {
    return new Version(normal, preRelease, VersionParser.parseBuild(build));
  }

  /**
   * Checks if this version is greater than the other version.
   *
   * @param other the other version to compare to
   * @return {@code true} if this version is greater than the other version
   *     or {@code false} otherwise
   * @see #compareTo(Version other)
   */
  public boolean greaterThan(Version other) {
    return compareTo(other) > 0;
  }

  /**
   * Checks if this version is greater than or equal to the other version.
   *
   * @param other the other version to compare to
   * @return {@code true} if this version is greater than or equal
   *     to the other version or {@code false} otherwise
   * @see #compareTo(Version other)
   */
  public boolean greaterThanOrEqualTo(Version other) {
    return compareTo(other) >= 0;
  }

  /**
   * Checks if this version is less than the other version.
   *
   * @param other the other version to compare to
   * @return {@code true} if this version is less than the other version
   *     or {@code false} otherwise
   * @see #compareTo(Version other)
   */
  public boolean lessThan(Version other) {
    return compareTo(other) < 0;
  }

  /**
   * Checks if this version is less than or equal to the other version.
   *
   * @param other the other version to compare to
   * @return {@code true} if this version is less than or equal
   *     to the other version or {@code false} otherwise
   * @see #compareTo(Version other)
   */
  public boolean lessThanOrEqualTo(Version other) {
    return compareTo(other) <= 0;
  }

  /**
   * Checks if this version is compatible with the
   * other version in terms of their major versions.
   *
   * <p>When checking compatibility no assumptions
   * are made about the versions' precedence.
   *
   * @param other the other version to check with
   * @return {@code true} if this version is compatible with
   *     the other version or {@code false} otherwise
   * @since 0.10.0
   */
  public boolean isMajorVersionCompatible(Version other) {
    return this.getMajorVersion() == other.getMajorVersion();
  }

  /**
   * Checks if this version is compatible with the
   * other version in terms of their minor versions.
   *
   * <p>When checking compatibility no assumptions
   * are made about the versions' precedence.
   *
   * @param other the other version to check with
   * @return {@code true} if this version is compatible with
   *     the other version or {@code false} otherwise
   * @since 0.10.0
   */
  public boolean isMinorVersionCompatible(Version other) {
    return this.getMajorVersion() == other.getMajorVersion()
        && this.getMinorVersion() == other.getMinorVersion();
  }

  /**
   * Checks if this version equals the other version.
   *
   * <p>The comparison is done by the {@code Version.compareTo} method.
   *
   * @param other the other version to compare to
   * @return {@code true} if this version equals the other version
   *     or {@code false} otherwise
   * @see #compareTo(Version other)
   */
  @Override
  public boolean equals(Object other) {
    return this == other || other instanceof Version && compareTo((Version) other) == 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int hash = 5;
    hash = 97 * hash + normal.hashCode();
    hash = 97 * hash + preRelease.hashCode();
    return hash;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getNormalVersion());
    if (!getPreReleaseVersion().isEmpty()) {
      sb.append(PRE_RELEASE_PREFIX).append(getPreReleaseVersion());
    }
    if (!getBuildMetadata().isEmpty()) {
      sb.append(BUILD_PREFIX).append(getBuildMetadata());
    }
    return sb.toString();
  }

  /**
   * Compares this version to the other version.
   *
   * <p>This method does not take into account the versions' build
   * metadata. If you want to compare the versions' build metadata
   * use the {@code Version.compareWithBuildsTo} method or the
   * {@code Version.BUILD_AWARE_ORDER} comparator.
   *
   * @param other the other version to compare to
   * @return a negative integer, zero or a positive integer if this version
   *     is less than, equal to or greater the the specified version
   * @see #BUILD_AWARE_ORDER
   * @see #compareWithBuildsTo(Version other)
   */
  @Override
  public int compareTo(Version other) {
    int result = normal.compareTo(other.normal);
    if (result == 0) {
      result = preRelease.compareTo(other.preRelease);
    }
    return result;
  }

  /**
   * Compare this version to the other version
   * taking into account the build metadata.
   *
   * <p>The method makes use of the {@code Version.BUILD_AWARE_ORDER} comparator.
   *
   * @param other the other version to compare to
   * @return integer result of comparison compatible with
   *     that of the {@code Comparable.compareTo} method
   * @see #BUILD_AWARE_ORDER
   */
  public int compareWithBuildsTo(Version other) {
    return BUILD_AWARE_ORDER.compare(this, other);
  }

  /**
   * A mutable builder for the immutable {@code Version} class.
   */
  public static class Builder {

    /**
     * The normal version string.
     */
    private String normal;

    /**
     * The pre-release version string.
     */
    private String preRelease;

    /**
     * The build metadata string.
     */
    private String build;

    /**
     * Constructs a {@code Builder} instance.
     */
    public Builder() {

    }

    /**
     * Constructs a {@code Builder} instance with the
     * string representation of the normal version.
     *
     * @param normal the string representation of the normal version
     */
    public Builder(String normal) {
      this.normal = normal;
    }

    /**
     * Sets the normal version.
     *
     * @param normal the string representation of the normal version
     * @return this builder instance
     */
    public Builder setNormalVersion(String normal) {
      this.normal = normal;
      return this;
    }

    /**
     * Sets the pre-release version.
     *
     * @param preRelease the string representation of the pre-release version
     * @return this builder instance
     */
    public Builder setPreReleaseVersion(String preRelease) {
      this.preRelease = preRelease;
      return this;
    }

    /**
     * Sets the build metadata.
     *
     * @param build the string representation of the build metadata
     * @return this builder instance
     */
    public Builder setBuildMetadata(String build) {
      this.build = build;
      return this;
    }

    /**
     * Builds a {@code Version} object.
     *
     * @return a newly built {@code Version} instance
     * @throws ParseException               when invalid version string is provided
     * @throws UnexpectedCharacterException is a special case of {@code ParseException}
     */
    public Version build() {
      StringBuilder sb = new StringBuilder();
      if (isFilled(normal)) {
        sb.append(normal);
      }
      if (isFilled(preRelease)) {
        sb.append(PRE_RELEASE_PREFIX).append(preRelease);
      }
      if (isFilled(build)) {
        sb.append(BUILD_PREFIX).append(build);
      }
      return VersionParser.parseValidSemVer(sb.toString());
    }

    /**
     * Checks if a string has a usable value.
     *
     * @param str the string to check
     * @return {@code true} if the string is filled or {@code false} otherwise
     */
    private boolean isFilled(String str) {
      return str != null && !str.isEmpty();
    }
  }

  /**
   * A build-aware comparator.
   */
  private static class BuildAwareOrder implements Comparator<Version> {

    /**
     * Compares two {@code Version} instances taking
     * into account their build metadata.
     *
     * <p>When compared build metadata is divided into identifiers. The
     * numeric identifiers are compared numerically, and the alphanumeric
     * identifiers are compared in the ASCII sort order.
     *
     * <p>If one of the compared versions has no defined build
     * metadata, this version is considered to have a lower
     * precedence than that of the other.
     *
     * @return {@inheritDoc}
     */
    @Override
    public int compare(Version v1, Version v2) {
      int result = v1.compareTo(v2);
      if (result == 0) {
        result = v1.build.compareTo(v2.build);
        if (v1.build == MetadataVersion.NULL
            || v2.build == MetadataVersion.NULL
        ) {
          /*
           * Build metadata should have a higher precedence
           * than the associated normal version which is the
           * opposite compared to pre-release versions.
           */
          result = -1 * result;
        }
      }
      return result;
    }
  }
}
