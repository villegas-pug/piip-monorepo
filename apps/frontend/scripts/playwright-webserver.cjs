const { spawn } = require('node:child_process');
const path = require('node:path');

const repoRoot = path.resolve(__dirname, '..');
const cliPath = path.join(repoRoot, 'node_modules', '@angular', 'cli', 'bin', 'ng.js');

const child = spawn(process.execPath, [cliPath, 'serve', ...process.argv.slice(2)], {
  cwd: repoRoot,
  env: process.env,
  stdio: 'inherit'
});

const stop = (signal) => {
  if (!child.killed) {
    child.kill(signal);
  }
};

process.on('SIGINT', () => stop('SIGINT'));
process.on('SIGTERM', () => stop('SIGTERM'));

child.on('exit', (code, signal) => {
  if (signal) {
    process.exit(1);
  }

  process.exit(code ?? 0);
});
