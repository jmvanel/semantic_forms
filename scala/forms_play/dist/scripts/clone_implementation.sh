
echo clone SF implementation, i.e. clone an unzipped SF distribution
#  use case: reuse the implementation with different databases or settings

CLONE=../semantic_forms_cloned
mkdir $CLONE

for f in en fr lib README.md  ; do rm -rf  $CLONE/$f ; ln -s $PWD/$f $CLONE/$f ; echo ln -s $PWD/$f $CLONE/$f ; done

for f in conf files scripts bin ; do rm -rf  $CLONE/$f ; cp -r $f $CLONE ; echo cp -r $f $CLONE ; done
echo current SF implementation cloned in $CLONE , you might want to rename it.
