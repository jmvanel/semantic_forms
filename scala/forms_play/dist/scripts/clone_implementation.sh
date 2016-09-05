
echo clone SF implementation, i.e. clone an unzipped SF distribution
#  use case: reuse the implementation with different databases or settings

mkdir ../semantic_forms_cloned

for f in en fr lib README.md scripts ; do ln -s $PWD/$f ../semantic_forms_cloned/$f ; echo ln -s $PWD/$f ../semantic_forms_cloned/$f ; done

for f in bin conf files ; do cp -r $f ../semantic_forms_cloned/ ; echo cp -r $f ../semantic_forms_cloned/ ; done
echo current SF implementation cloned in ../semantic_forms_cloned
