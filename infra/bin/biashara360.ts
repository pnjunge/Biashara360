#!/usr/bin/env node
import * as cdk from 'aws-cdk-lib'
import { Biashara360ApiStack } from '../lib/biashara360-api-stack'

const app = new cdk.App()
const account = app.node.tryGetContext('account')
const region = app.node.tryGetContext('region')

new Biashara360ApiStack(app, 'Biashara360ApiProduction', {
  env: { account, region },
  terminationProtection: true,
  description: 'Biashara360 production API on ECS Fargate with observability'
})
