import unittest
from audit_structure import gradle_namespace, declared_classes, component_available


class StructureFixtures(unittest.TestCase):
    def test_gradle_assignment_and_groovy_forms(self):
        for value in ("namespace = 'com.matrix.launcher'", 'namespace "com.matrix.launcher"'):
            self.assertEqual('com.matrix.launcher', gradle_namespace(value))
        with self.assertRaises(AssertionError):
            gradle_namespace('')

    def test_multiple_declarations_in_one_source(self):
        classes = declared_classes('package com.matrix.launcher\ninternal object AotOptimizationManager {}\nclass AotPackageUpdateReceiver {}')
        self.assertTrue(component_available('.AotPackageUpdateReceiver', 'com.matrix.launcher', classes))
        self.assertFalse(component_available('.MissingReceiver', 'com.matrix.launcher', classes))

    def test_only_explicit_dependency_component_is_exempt(self):
        self.assertTrue(component_available('androidx.core.content.FileProvider', 'com.matrix.launcher', {}))
        self.assertFalse(component_available('androidx.other.Missing', 'com.matrix.launcher', {}))


if __name__ == '__main__':
    unittest.main()
