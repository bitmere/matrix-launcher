import unittest
from matrix_ui import resumed_ui


class LiveDumpTest(unittest.TestCase):
    def test_stopped_task_does_not_supply_current_focus_or_stale_rows(self):
        dump = ('TASK old\n  ACTIVITY old\n    MatrixUi page=home\n    homeRow=3\n    lifecycle=CREATED\n'
                'TASK live\n  ACTIVITY live\n    MatrixUi page=home\n    homeRow=7\n    lifecycle=RESUMED\n')
        live = resumed_ui(dump)
        self.assertIn('homeRow=7', live)
        self.assertNotIn('homeRow=3', live)

    def test_missing_or_ambiguous_foreground_fails(self):
        with self.assertRaises(ValueError):
            resumed_ui('MatrixUi page=home\nlifecycle=CREATED')
        with self.assertRaises(ValueError):
            resumed_ui(('  ACTIVITY x\nMatrixUi page=home\nlifecycle=RESUMED\n') * 2)


if __name__ == '__main__':
    unittest.main()
