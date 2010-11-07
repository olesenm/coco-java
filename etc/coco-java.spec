Summary: The Compiler Generator Coco/R, Java version
Name: coco-java
Version: 20101106
Release: 2
License: GPL
Group:  Development/Languages/Java
Source0: https://github.com/downloads/olesenm/coco-java/coco-java-%{version}.tar.gz
URL: http://www.ssw.uni-linz.ac.at/coco/
Provides: coco-java
Requires: java
BuildRequires: java-sdk perl
BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)

%description
Coco/R is a compiler generator, which takes an attributed grammar of a
source language and generates a scanner and a parser for this
language. The scanner works as a deterministic finite automaton. The
parser uses recursive descent. LL(1) conflicts can be resolved by a
multi-symbol lookahead or by semantic checks. Thus the class of
accepted grammars is LL(k) for an arbitrary k.

%prep
%setup -n coco-java-%{version} -q

%build
%configure
make

%install
rm -rf $RPM_BUILD_ROOT
make DESTDIR=${RPM_BUILD_ROOT} install

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root)
%doc COPYING README.mkdn src/Coco/Coco-java.atg
%{_bindir}/*
%{_datadir}/*

%changelog
* Sun Nov  7 2010 Mark Olesen
- corrected category, build requires perl

* Sat Nov  6 2010 Mark Olesen
- created spec file
