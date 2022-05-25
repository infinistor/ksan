#
PYTHON=`which python`
DESTDIR=/

all:
	@echo "make source - Create source package"
	@echo "make buildrpm - Generate a rpm package"
	@echo "make clean - Get rid of scratch and byte files"

source:
	$(PYTHON) setup.py sdist $(COMPILE)

buildrpm:
	$(PYTHON) setup.py bdist_rpm --release=$(shell echo `git rev-list --all --count`) --force-arch=x86_64

clean:
	$(PYTHON) setup.py clean
	rm -rf build/
	rm -rf InfiniStor_WATCHER.egg-info
	find . -name '*.pyc' -delete